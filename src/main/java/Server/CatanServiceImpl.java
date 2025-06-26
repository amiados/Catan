package Server;

import Model.InviteStatus;
import Model.MemberRole;
import Model.DAO.*;
import Model.OBJ.*;
import Security.PasswordHasher;
import Utils.*;
import catan.Catan;
import catan.CatanServiceGrpc;
import catan.PieceColor;
import com.google.common.cache.Cache;
import io.grpc.Context;
import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CatanServiceImpl extends CatanServiceGrpc.CatanServiceImplBase {

    private final Cache<String, OTP_Entry> otpCache;
    private final Cache<String, User> pendingRegistrations;
    private final Cache<String, User> pendingLogin;

    private final UserDAO userDAO;
    private final MessageDAO messageDAO;
    private final InviteDAO inviteDAO;
    private final GameDAO gameDAO;
    private final PlayerDAO playerDAO;
    private final GroupDAO groupDAO;
    private final GroupMemberDAO groupMemberDAO;

    private final ConcurrentMap<UUID, ConcurrentMap<UUID, StreamObserver<Catan.GroupChatMessage>>> groupSubs =
            new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, ConcurrentMap<UUID, StreamObserver<Catan.GameChatMessage>>> gameSubs =
            new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, String> usernameCache = new ConcurrentHashMap<>();

    private final LobbyEventManager lobbyEventManager = new LobbyEventManager();

    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final int LOCK_DURATION_MINUTES = 10;
    private final OTPManager otpManager = new OTPManager();

    public CatanServiceImpl(Cache<String, OTP_Entry> otpCache, Cache<String, User> pendingRegistrations, Cache<String, User> pendingUsers,
                            UserDAO userDAO, MessageDAO messageDAO, InviteDAO inviteDAO, GameDAO gameDAO, PlayerDAO playerDAO, GroupDAO groupDAO, GroupMemberDAO groupMemberDAO) {
        this.otpCache = otpCache;
        this.pendingRegistrations = pendingRegistrations;
        this.pendingLogin = pendingUsers;
        this.userDAO = userDAO;
        this.messageDAO = messageDAO;
        this.inviteDAO = inviteDAO;
        this.gameDAO = gameDAO;
        this.playerDAO = playerDAO;
        this.groupDAO = groupDAO;
        this.groupMemberDAO = groupMemberDAO;
    }

    @Override
    public void register(Catan.RegisterRequest request, StreamObserver<Catan.ConnectionResponse> responseObserver) {
        try {
            String username = request.getUsername();
            String email = request.getEmail();
            String password = request.getPassword();

            ValidationResult vr = User.validate(username, email, password);
            if (!vr.isValid()) {
                respondConnection(responseObserver, false,
                        "Invalid registration data",
                        null,
                        vr.getMessages(),
                        null);
                return;
            }

            if (isEmailTaken(email)) {
                respondConnection(responseObserver, false,
                        "Invalid registration data",
                        null,
                        List.of("Invalid input"),
                        null);
                return;
            }

            String otp = EmailSender.generateOTP();

            if (!EmailSender.sendOTP(email, otp)) {
                respondConnection(responseObserver, false,
                        "Failed to send OTP",
                        null,
                        null,
                        null);
                return;
            }

            otpCache.put(email, new OTP_Entry(email, otp));

            User newUser = new User(username, email, password);

            pendingRegistrations.put(email, newUser);

            respondConnection(responseObserver, true,
                    "OTP sent—valid for 5 minutes",
                    null,
                    null,
                    null);
        } catch (Exception e) {
            e.printStackTrace();
            respondConnection(responseObserver, false,
                    "Failed to register user, please try again later",
                    null,
                    List.of("Unexpected error"),
                    null);
        }
    }

    @Override
    public void login(Catan.LoginRequest request, StreamObserver<Catan.ConnectionResponse> responseObserver) {
        String email = request.getEmail();
        String password = request.getPassword();

        ValidationResult vr = User.validate(email, password);
        if (!vr.isValid()) {
            respondConnection(responseObserver, false,
                    "Invalid login data",
                    null,
                    vr.getMessages(),
                    null);
            return;
        }

        User user;
        try {
            user = userDAO.getUserByEmail(email);
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            respondConnection(responseObserver, false,
                    "Server error",
                    null,
                    List.of("Please try again later"),
                    null);
            return;
        }

        if (user == null) {
            respondConnection(responseObserver, false,
                    "Invalid credentials",
                    null,
                    List.of("Authentication failed"),
                    null);
            return;
        }

        if (user.isLocked()) {
            responseObserver.onError(Status.PERMISSION_DENIED
                    .withDescription("Account locked until " + user.getLockUntil())
                    .asRuntimeException());
            return;
        }

        if (!PasswordHasher.verify(password, user.getPasswordHash())) {
            int fails = user.getFailedLogins() + 1;
            user.setFailedLogins(fails);
            if (fails >= MAX_FAILED_ATTEMPTS) {
                user.setLockUntil(Instant.now().plus(LOCK_DURATION_MINUTES, ChronoUnit.MINUTES));
            }

            userDAO.updateUser(user);
            respondConnection(responseObserver, false,
                    "Invalid credentials",
                    user.getUsername(),
                    List.of("Authentication failed"),
                    user.getId().toString());
        }

        user.setFailedLogins(0);
        user.setLockUntil(null);

        try {
            String otp = EmailSender.generateOTP();
            if (!EmailSender.sendOTP(email, otp)) {
                respondConnection(responseObserver, false,
                        "Failed to send OTP",
                        user.getUsername(),
                        null,
                        user.getId().toString());
                return;
            }

            otpCache.put(email, new OTP_Entry(email, otp));
            pendingLogin.put(email, user);

            respondConnection(responseObserver, true,
                    "OTP sent to your email",
                    user.getUsername(),
                    null,
                    user.getId().toString());
        } catch (Exception e) {
            e.printStackTrace();
            respondConnection(responseObserver, false,
                    "Eailed to create session",
                    user.getUsername(),
                    List.of("Unexpected error"),
                    user.getId().toString());
        }
    }

    @Override
    public void verifyRegisterOtp(Catan.VerifyOtpRequest request, StreamObserver<Catan.ConnectionResponse> responseObserver) {
        verifyOtp(request, responseObserver, pendingRegistrations, true);
    }

    @Override
    public void verifyLoginOtp(Catan.VerifyOtpRequest request, StreamObserver<Catan.ConnectionResponse> responseObserver) {
        verifyOtp(request, responseObserver, pendingLogin, false);
    }

    private void verifyOtp(Catan.VerifyOtpRequest request, StreamObserver<Catan.ConnectionResponse>responseObserver, Cache <String, User> userCache, boolean isRegistration) {
        String email = request.getEmail();
        String otp = request.getOtp();

        User user = userCache.getIfPresent(email);
        if (user == null) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("User not found with email: " + email)
                            .asRuntimeException()
            );
            return;
        }

        OTP_Entry entry = otpCache.getIfPresent(email);
        if (entry == null || !entry.isValid(otp)) {
            respondConnection(responseObserver, false,
                    "Invalid or expired OTP",
                    null,
                    null,
                    null);
            return;
        }

        try {
            if (isRegistration) {
                userDAO.createUser(user);
            } else {
                userDAO.updateUserLoginState(user);
            }
            otpCache.invalidate(email);
            userCache.invalidate(email);

            respondConnection(responseObserver, true,
                    (isRegistration ? "Registration complete" : "Login successful"),
                    user.getUsername(),
                    null,
                    user.getId().toString());
        } catch (Exception e) {
            e.printStackTrace();
            respondConnection(responseObserver, false,
                    "Internal server error",
                    user.getUsername(),
                    List.of("Unexpected error"),
                    user.getId().toString());
        }
    }

    @Override
    public void sendRegisterOtp(Catan.EmailRequest request, StreamObserver<Catan.ConnectionResponse> responseObserver) {
        String email = request.getEmail().trim();
        Catan.ConnectionResponse.Builder response = Catan.ConnectionResponse.newBuilder();

        User user = pendingRegistrations.getIfPresent(email);
        // בדיקה אם יש בקשת רישום ממתינה
        if (user == null) {
            response.setSuccess(false)
                    .addErrors("No registration initiated")
                    .setMessage("Please fill registration form first.");
        }
        // בדיקה אם מותר לשלוח OTP
        else {
            boolean otpSent = otpManager.requestOTP(email);
            if (otpSent) {
                response.setSuccess(true)
                        .setMessage("OTP sent successfully to " + email);
            } else {
                response.setSuccess(false)
                        .addErrors("OTP already sent or locked")
                        .setMessage("Try again later or check your inbox.");
            }
        }

        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

    @Override
    public void sendLoginOtp(Catan.EmailRequest request, StreamObserver<Catan.ConnectionResponse> responseObserver) {
        String email = request.getEmail().trim();
        Catan.ConnectionResponse.Builder response = Catan.ConnectionResponse.newBuilder();

        try {
            User existingUser = userDAO.getUserByEmail(email);
            if (existingUser == null) {
                response.setSuccess(false)
                        .addErrors("User not found")
                        .setMessage("No user with this email.");
            } else {
                pendingLogin.put(email, existingUser); // שומר את המשתמש בזיכרון זמני
                boolean otpSent = otpManager.requestOTP(email);
                if (otpSent) {
                    response.setSuccess(true)
                            .setMessage("OTP sent successfully to " + email);
                } else {
                    response.setSuccess(false)
                            .addErrors("OTP already sent or locked")
                            .setMessage("Try again later or check your inbox.");
                }
            }
        }  catch (SQLException | IOException e) {
            response.setSuccess(false)
                    .addErrors("Internal server error")
                    .setMessage("Database or IO error while fetching user.");
            e.printStackTrace();
        }

        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getAllGroups(Catan.UserRequest request, StreamObserver<Catan.AllGroupsResponse> responseObserver) {
        UUID userId;
        try {
            userId = UUID.fromString(request.getUserId());
        } catch (IllegalArgumentException e) {
            responseObserver.onNext(Catan.AllGroupsResponse.newBuilder()
                    .build());
            responseObserver.onCompleted();
            return;
        }
        try {
            List<Group> groups = groupDAO.getGroupsForUser(userId);
            Catan.AllGroupsResponse.Builder builder = Catan.AllGroupsResponse.newBuilder();

            for (Group group : groups) {
                UUID creatorId = group.getCreatorId();

                String creatorUsername = usernameCache.computeIfAbsent(creatorId, id -> {
                    try {
                        return userDAO.getUsernameById(id);
                    } catch (SQLException e) {
                        e.printStackTrace();
                        return "Unknown";
                    }
                });

                builder.addGroups(Catan.GroupInfo.newBuilder()
                        .setGroupId(group.getGroupId().toString())
                        .setGroupName(group.getGroupName())
                        .setCreatorUsername(creatorUsername)
                        .build());
            }
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (SQLException e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("שגיאה בשליפת קבוצות")
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    public void createGroup(Catan.CreateGroupRequest request, StreamObserver<Catan.GroupResponse> responseObserver) {
        UUID creatorId;
        try {
            creatorId = UUID.fromString(request.getCreatorId());
        } catch (IllegalArgumentException ex) {
            responseObserver.onNext(Catan.GroupResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Invalid UUID format for creatorId")
                    .build());
            responseObserver.onCompleted();
            return;
        }

        String groupName = request.getGroupName();

        // בדיקת תקינות השם
        if (groupName.isBlank()) {
            responseObserver.onNext(Catan.GroupResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Group name cannot be empty")
                    .build());
            responseObserver.onCompleted();
            return;
        }

        try {
            User creator = userDAO.getUserById(creatorId);
            if (creator == null) {
                responseObserver.onNext(Catan.GroupResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Invalid UUID format for creatorId")
                        .build()
                );
                responseObserver.onCompleted();
                return;
            }

            UUID groupId = UUID.randomUUID();
            Group group = new Group(groupId, Instant.now(), creatorId, groupName);

            if (!groupDAO.createGroup(group)) {
                responseObserver.onNext(Catan.GroupResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Failed to create group")
                        .build()
                );
            } else {
                groupMemberDAO.addMemberToGroup(groupId, creatorId, MemberRole.ADMIN);

                responseObserver.onNext(Catan.GroupResponse.newBuilder()
                        .setSuccess(true)
                        .setMessage("Group created successfully")
                        .setGroupId(groupId.toString())
                        .build());
            }

            responseObserver.onCompleted();

        } catch (SQLException e) {
            e.printStackTrace();
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Database error: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void inviteToGroup(Catan.InviteRequest request, StreamObserver<Catan.GroupResponse> responseObserver) {
        UUID inviteId, groupId, senderId, receiverId;
        // === בדיקות תקינות של מזהי UUID ===
        try {
            inviteId = UUID.fromString(request.getInviteId());
            groupId = UUID.fromString(request.getGroupId());
            senderId = UUID.fromString(request.getSenderId());
            receiverId = UUID.fromString(request.getReceiverId());
        } catch (IllegalArgumentException ex) {
            responseObserver.onNext(Catan.GroupResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Invalid UUID format: " + ex.getMessage())
                    .build());
            responseObserver.onCompleted();
            return;
        }

        if (senderId.equals(receiverId)) {
            responseObserver.onNext(Catan.GroupResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Cannot invite yourself to a group")
                    .build());
            responseObserver.onCompleted();
            return;
        }

        try {
            // === בדיקת קיום קבוצה ===
            Group group = groupDAO.getGroupById(groupId);
            if (group == null) {
                responseObserver.onNext(Catan.GroupResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Group does not exist")
                        .build());
                responseObserver.onCompleted();
                return;
            }

            // === בדיקת קיום משתמשים ===
            User sender = userDAO.getUserById(senderId);
            if (sender == null) {
                responseObserver.onNext(Catan.GroupResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Sender does not exist")
                        .build());
                responseObserver.onCompleted();
                return;
            }

            User receiver = userDAO.getUserById(receiverId);
            if (receiver == null) {
                responseObserver.onNext(Catan.GroupResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Receiver does not exist")
                        .build());
                responseObserver.onCompleted();
                return;
            }

            // === בדיקה האם השולח חבר בקבוצה או בעל הרשאה ===
            if (!groupMemberDAO.isUserInGroup(groupId, senderId)) {
                responseObserver.onNext(Catan.GroupResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("User " + senderId + " is not a member of group " + groupId)
                        .build());
                responseObserver.onCompleted();
                return;
            }

            // === בדיקה אם כבר קיימת הזמנה פעילה ===
            Invite existingInvite = inviteDAO.getInvite(groupId, receiverId);
            if (existingInvite != null && existingInvite.getStatus() == InviteStatus.PENDING) {
                responseObserver.onNext(Catan.GroupResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("A pending invite already exists")
                        .build());
                responseObserver.onCompleted();
                return;
            }

            Instant timestamp = Instant.now();
            Invite newInvite = new Invite(inviteId, groupId, senderId, receiverId, InviteStatus.PENDING, timestamp);
            inviteDAO.createInvite(newInvite);

            responseObserver.onNext(Catan.GroupResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Invite sent successfully")
                    .setGroupId(groupId.toString())
                    .build());
            responseObserver.onCompleted();

        } catch (SQLException e) {
            e.printStackTrace();
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Database error: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void respondToGroupInvitation(Catan.InviteResponse request, StreamObserver<Catan.GroupResponse> responseObserver) {
        UUID inviteId, groupId, senderId, receiverId;
        try {
            inviteId = UUID.fromString(request.getInviteId());
            groupId = UUID.fromString(request.getGroupId());
            senderId = UUID.fromString(request.getSenderId());
            receiverId = UUID.fromString(request.getReceiverId());
        } catch (IllegalArgumentException ex) {
            responseObserver.onNext(Catan.GroupResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Invalid UUID format: " + ex.getMessage())
                    .build());
            responseObserver.onCompleted();
            return;
        }

        try {
            // בדוק שההזמנה קיימת
            Invite invite = inviteDAO.getInviteById(inviteId);
            if (invite == null) {
                responseObserver.onNext(Catan.GroupResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Invite does not exist")
                        .build());
                responseObserver.onCompleted();
                return;
            }

            // בדוק שההזמנה היא למשתמש המבצע
            if (!invite.getReceiverId().equals(receiverId)) {
                responseObserver.onNext(Catan.GroupResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("You are not the intended recipient of this invite")
                        .build());
                responseObserver.onCompleted();
                return;
            }

            if (!invite.getSenderId().equals(senderId) ||
                    !groupMemberDAO.isUserInGroup(groupId, senderId) ||
                    groupMemberDAO.getUserRole(groupId, senderId) != MemberRole.ADMIN) {
                responseObserver.onNext(Catan.GroupResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("You are not the intended recipient of this invite")
                        .build());
                responseObserver.onCompleted();
                return;
            }

            // עדכן סטטוס במסד
            InviteStatus newStatus = InviteStatus.valueOf(request.getStatus().name());
            inviteDAO.updateInviteStatus(inviteId, newStatus);

            // אם ההזמנה התקבלה, הוסף את המשתמש לקבוצה
            if (newStatus == InviteStatus.ACCEPTED) {
                groupMemberDAO.addMemberToGroup(groupId, receiverId, MemberRole.MEMBER);
            }

            responseObserver.onNext(Catan.GroupResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Invite response processed")
                    .setGroupId(groupId.toString())
                    .build());
            responseObserver.onCompleted();

        } catch (SQLException e) {
            e.printStackTrace();
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Database error: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getUserInvites(Catan.UserRequest request, StreamObserver<Catan.InviteListResponse> responseObserver) {
        UUID userId;
        try {
            userId = UUID.fromString(request.getUserId());
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid UUID format for userId")
                    .asRuntimeException());
            return;
        }

        try {
            if (userDAO.getUserById(userId) == null) {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("User not found")
                        .asRuntimeException());
                return;
            }

            List<Invite> invites = inviteDAO.getUserPendingInvites(userId);
            Catan.InviteListResponse.Builder responseBuilder = Catan.InviteListResponse.newBuilder();

            for (Invite invite : invites) {
                responseBuilder.addInvites(Catan.ProtoInvite.newBuilder()
                        .setInviteId(invite.getId().toString())
                        .setGroupId(invite.getGroupId().toString())
                        .setSenderId(invite.getSenderId().toString())
                        .setInvitedUserId(invite.getReceiverId().toString())
                        .setTimestamp(invite.getSentAt().toEpochMilli())
                        .setStatus(Catan.InviteResponseStatus.valueOf(invite.getStatus().name()))
                        .build());
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (SQLException e) {
            e.printStackTrace();
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Database error: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void listGroupGames(Catan.GroupRequest request, StreamObserver<Catan.GameListResponse> responseObserver) {
        // 1. Parse and validate the incoming groupId
        UUID groupId;
        try {
            groupId = UUID.fromString(request.getGroupId());
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid UUID format for groupId")
                    .asRuntimeException());
            return;
        }

        try {
            // 2. Verify that the group actually exists
            Group group = groupDAO.getGroupById(groupId);
            if (group == null) {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("Group not found: " + groupId)
                        .asRuntimeException()
                );
                return;
            }

            // 3. Fetch all games for this group
            List<Game> games = gameDAO.getGamesByGroupId(groupId);

            // 4. Build the response listing just the game IDs
            Catan.GameListResponse.Builder respond = Catan.GameListResponse.newBuilder();
            for (Game game : games) {
                respond.addGameIds(game.getGameId().toString());
            }

            // 5. Send back and complete
            responseObserver.onNext(respond.build());
            responseObserver.onCompleted();
        } catch (SQLException e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("DB error: " + e.getMessage())
                    .asRuntimeException()
            );
        }
    }

    @Override
    public void getGamePlayers(Catan.GameActionRequest request, StreamObserver<Catan.GamePlayersResponse> responseObserver) {
        UUID gameId, userId;
        try {
            gameId = UUID.fromString(request.getGameId());
            userId = UUID.fromString(request.getUserId());
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid UUID format for: " + e.getMessage())
                    .asRuntimeException()
            );
            return;
        }

        try {
            User user = userDAO.getUserById(userId);
            if (user == null) {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("User not found: " + userId)
                        .asRuntimeException()
                );
                return;
            }
            Game game = gameDAO.getGameById(gameId);
            if (game == null) {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("Game not found: " + gameId)
                        .asRuntimeException()
                );
                return;
            }

            List<Game> gameList = gameDAO.getGamesByUserId(userId);

            boolean isInGame = gameList.contains(game);
            if (!isInGame) {
                responseObserver.onError(Status.PERMISSION_DENIED
                        .withDescription("You are not a participant in this game")
                        .asRuntimeException());
                return;
            }

            List<Player> players = playerDAO.getPlayersByGame(gameId);
            Catan.GamePlayersResponse.Builder respond = Catan.GamePlayersResponse.newBuilder();
            for (Player player : players) {
                respond.addPlayers(Catan.PlayerProto.newBuilder()
                        .setPlayerId(player.getPlayerId().toString())
                        .setUsername(player.getUsername())
                        .setColor(player.getPieceColor().name())
                        .setIsTurn(player.isTurn())
                        .build()
                );
            }

            responseObserver.onNext(respond.build());
            responseObserver.onCompleted();

        } catch (SQLException e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("DB error: " + e.getMessage())
                    .asRuntimeException()
            );
        }
    }

    @Override
    public void sendGroupMessage(Catan.GroupChatMessage request, StreamObserver<Catan.ACK> responseObserver) {
        UUID groupId, senderId;
        try {
            groupId = UUID.fromString(request.getGroupId());
            senderId = UUID.fromString(request.getFromUserId());
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid UUID: " + ex.getMessage())
                    .asRuntimeException());
            return;
        }
        GroupMessage msg = new GroupMessage(
                UUID.randomUUID(),
                groupId,
                senderId,
                request.getContent().toByteArray(),
                Instant.ofEpochMilli(request.getTimestamp())
        );
        try {
            messageDAO.saveGroupMessage(msg);
        } catch (SQLException e) {
            e.printStackTrace();
            responseObserver.onError(Status.INTERNAL
                    .withDescription("DB error: " + e.getMessage())
                    .asRuntimeException());
            return;
        }

        // שליפת שם המשתמש
        String senderName = usernameCache.get(senderId);
        if (senderName == null) {
            try {
                senderName = userDAO.getUsernameById(senderId);
                usernameCache.put(senderId, senderName);
            } catch (SQLException e) {
                e.printStackTrace();
                responseObserver.onError(Status.INTERNAL
                        .withDescription("Failed to fetch username: " + e.getMessage())
                        .asRuntimeException());
                return;
            }
        }

        Catan.GroupChatMessage enrichedMessage = Catan.GroupChatMessage.newBuilder()
                .setGroupId(groupId.toString())
                .setFromUserId(senderId.toString())
                .setFromUsername(senderName)
                .setContent(request.getContent())
                .setTimestamp(request.getTimestamp())
                .build();

        // דחיפת ההודעה לכולם
        // Push message to all subscribers (excluding sender if you want)
        ConcurrentMap<UUID, StreamObserver<Catan.GroupChatMessage>> groupMap = groupSubs.get(groupId);
        if (groupMap != null) {
            groupMap.forEach((uid, obs) -> {
                try {
                    boolean shouldSend = true;

                    if (obs instanceof ServerCallStreamObserver<?> scso) {
                        ServerCallStreamObserver<?> observer = (ServerCallStreamObserver<?>) obs;
                        if (observer.isCancelled()) {
                            shouldSend = false;
                            groupMap.remove(uid);
                            System.out.println("❌ Stream for user " + uid + " is cancelled. Removed from group " + groupId);
                        }
                    }

                    if (shouldSend && !uid.equals(senderId)) {
                        obs.onNext(enrichedMessage);
                    }
                } catch (Exception e) {
                    groupMap.remove(uid);
                    System.out.println("⚠️ Failed to send to user " + uid + ": " + e.getMessage());
                }
            });
        }
        // אישור שולח
        responseObserver.onNext(Catan.ACK.newBuilder().setReceived(true).build());
        responseObserver.onCompleted();
    }

    @Override
    public void subscribeGroupMessages(Catan.GroupSubscribeRequest req,
                                       StreamObserver<Catan.GroupChatMessage> obs) {
        UUID groupId, userId;
        try {
            groupId = UUID.fromString(req.getGroupId());
            userId  = UUID.fromString(req.getUserId());
        } catch (IllegalArgumentException ex) {
            obs.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid UUID: " + ex.getMessage())
                    .asRuntimeException());
            return;
        }
        try {
            // כאן צריך לבדוק שהמשתמש חבר בקבוצה; לדוגמה:
            if (!groupMemberDAO.isUserInGroup(groupId, userId)) {
                obs.onError(Status.PERMISSION_DENIED
                        .withDescription("User " + userId + " is not a member of group " + groupId)
                        .asRuntimeException());
                return;
            }

            groupSubs
                    .computeIfAbsent(groupId, id -> new ConcurrentHashMap<>())
                    .put(userId, obs);

            // הסרת המנוי כשלקוח מתנתק
            Context.current().addListener(context -> {
                ConcurrentMap<UUID, StreamObserver<Catan.GroupChatMessage>> m = groupSubs.get(groupId);
                if (m != null) m.remove(userId);
            }, Runnable::run);
        } catch (SQLException e) {
            obs.onError(Status.INTERNAL
                    .withDescription("DB error: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void sendGameMessage(Catan.GameChatMessage request, StreamObserver<Catan.ACK> responseObserver) {
        UUID gameId, senderId;
        try {
            gameId = UUID.fromString(request.getGameId());
            senderId = UUID.fromString(request.getFromUserId());
        } catch (IllegalArgumentException ex) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid UUID: " + ex.getMessage())
                    .asRuntimeException());
            return;
        }

        try {
            GameMessage msg = new GameMessage(
                    UUID.randomUUID(),
                    gameId,
                    senderId,
                    request.getContent().toByteArray(),
                    Instant.ofEpochMilli(request.getTimestamp())
            );

            messageDAO.saveGameMessage(msg);
        } catch (SQLException e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("DB error: " + e.getMessage())
                    .asRuntimeException());
        }

        ConcurrentMap<UUID, StreamObserver<Catan.GameChatMessage>> map = gameSubs.get(gameId);
        if (map != null) {
            map.forEach((uid, obs) -> {
                try {
                    if (uid != senderId) {
                        obs.onNext(request);
                    }
                } catch (Exception e) {
                    // Optionally: remove
                }
            });
        }

        responseObserver.onNext(Catan.ACK.newBuilder().setReceived(true).build());
        responseObserver.onCompleted();
    }

    @Override
    public void subscribeGameMessages(Catan.GameSubscribeRequest request,
                                      StreamObserver<Catan.GameChatMessage> responseObserver) {
        UUID gameId, userId;
        try {
            gameId = UUID.fromString(request.getGameId());
            userId = UUID.fromString(request.getUserId());
        } catch (IllegalArgumentException ex) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid UUID: " + ex.getMessage())
                    .asRuntimeException());
            return;
        }

        try {
            Game game = gameDAO.getGameById(gameId);

            if (game == null) {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("Game not found: " + gameId)
                        .asRuntimeException()
                );
                return;
            }

            if (!gameDAO.getGamesByUserId(userId).contains(game)) {
                responseObserver.onError(Status.PERMISSION_DENIED
                        .withDescription("User is not in game")
                        .asRuntimeException());
                return;
            }

            gameSubs.computeIfAbsent(gameId, id -> new ConcurrentHashMap<>()).put(userId, responseObserver);

            Context.current().addListener(ctx -> {
                ConcurrentMap<UUID, StreamObserver<Catan.GameChatMessage>> m = gameSubs.get(gameId);
                if (m != null) m.remove(userId);
            }, Runnable::run);
        } catch (SQLException e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("DB error: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void createGame(Catan.GameActionRequest request, StreamObserver<Catan.GameResponse> responseObserver) {
        UUID groupId, creatorId, gameId;
        Instant timeStamp;
        try {
            groupId = UUID.fromString(request.getGroupId());
            creatorId = UUID.fromString(request.getUserId());
            gameId = UUID.fromString(request.getGameId());
            timeStamp = Instant.ofEpochMilli(request.getTimestamp());
        } catch (IllegalArgumentException ex) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid UUID: " + ex.getMessage())
                    .asRuntimeException());
            return;
        }
        try {
            // ודא שהקבוצה קיימת
            Group group = groupDAO.getGroupById(groupId);
            if (group == null) {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("Group not found")
                        .asRuntimeException());
                return;
            }

            Player creator = playerDAO.getPlayerById(creatorId);
            if (creator == null) {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("Player not found")
                        .asRuntimeException());
                return;
            }

            if (!groupMemberDAO.isUserInGroup(groupId, creatorId)) {
                responseObserver.onError(Status.PERMISSION_DENIED
                        .withDescription("User " + creatorId + " is not a member of group " + groupId)
                        .asRuntimeException());
                return;
            }

            // צור את המשחק
            Game game = new Game(gameId, groupId, timeStamp, null, null, creatorId);
            boolean gameCreated = gameDAO.createGame(game);

            if (!gameCreated) {
                responseObserver.onNext(Catan.GameResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Failed to create game")
                        .build());
                responseObserver.onCompleted();
                return;
            }

            // הוסף את יוצר המשחק כשחקן
            playerDAO.addPlayerToGame(creator, gameId);

            // שלח תגובה
            responseObserver.onNext(Catan.GameResponse.newBuilder()
                    .setSuccess(true)
                    .setGameId(gameId.toString())
                    .setMessage("Game created successfully")
                    .build());
            responseObserver.onCompleted();

        } catch (SQLException e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("DB error: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void startGame(Catan.GameActionRequest request, StreamObserver<Catan.GameResponse> responseObserver) {
        UUID groupId, gameId, userId;
        try {
            groupId = UUID.fromString(request.getGroupId());
            gameId = UUID.fromString(request.getGameId());
            userId = UUID.fromString(request.getUserId());
        } catch (IllegalArgumentException ex) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid UUID: " + ex.getMessage())
                    .asRuntimeException());
            return;
        }

        Instant startedAt;
        try {
            startedAt = Instant.ofEpochMilli(request.getTimestamp());
        } catch (IllegalArgumentException ex) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid time: " + ex.getMessage())
                    .asRuntimeException());
            return;
        }

        try {
            User user = userDAO.getUserById(userId);
            if (user == null) {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("User not found")
                        .asRuntimeException());
                return;
            }

            Group group = groupDAO.getGroupById(groupId);
            if (group == null) {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("Group not found")
                        .asRuntimeException());
                return;
            }

            if (!groupMemberDAO.isUserInGroup(groupId, userId)) {
                responseObserver.onError(Status.PERMISSION_DENIED
                        .withDescription("User " + userId + " is not a member of group " + groupId)
                        .asRuntimeException());
                return;
            }

            Game game = gameDAO.getGameById(gameId);
            if (game == null) {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("Game not found")
                        .asRuntimeException());
                return;
            }

            boolean started = gameDAO.startGame(gameId, startedAt);
            if (started) {
                lobbyEventManager.broadcast(gameId,
                        Catan.GameEvent.newBuilder()
                            .setType("START")
                            .setDescription("Game started")
                            .setTimestamp(startedAt.toEpochMilli())
                            .build());

                responseObserver.onNext(Catan.GameResponse.newBuilder()
                        .setSuccess(true)
                        .setGameId(gameId.toString())
                        .setMessage("Game started successfully")
                        .build());
            } else {
                responseObserver.onNext(Catan.GameResponse.newBuilder()
                        .setSuccess(false)
                        .setGameId(gameId.toString())
                        .setMessage("Game already started or failed to start")
                        .build());
            }
            responseObserver.onCompleted();

        } catch (SQLException e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("DB error: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void leaveGame(Catan.GameActionRequest request, StreamObserver<Catan.GameResponse> responseObserver) {
        UUID gameId, userId;
        try {
            gameId = UUID.fromString(request.getGameId());
            userId = UUID.fromString(request.getUserId());
        } catch (IllegalArgumentException ex) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid UUID: " + ex.getMessage())
                    .asRuntimeException());
            return;
        }


    }

    @Override
    public void endGame(Catan.GameActionRequest request, StreamObserver<Catan.GameResponse> responseObserver) {
        super.endGame(request, responseObserver);
    }

    @Override
    public void updatePlayerColor(Catan.UpdateColorRequest request, StreamObserver<Catan.GameResponse> responseObserver) {
        UUID playerId, gameId;
        try {
            playerId = UUID.fromString(request.getPlayerId());
            gameId = UUID.fromString(request.getGameId());
        } catch (IllegalArgumentException ex) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid UUID: " + ex.getMessage())
                    .asRuntimeException());
            return;
        }
        PieceColor pieceColor;
        try {
            pieceColor = ColorMapper.fromProtoColor(request.getColor());
        } catch (IllegalArgumentException ex) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid color: " + ex.getMessage())
                    .asRuntimeException());
            return;
        }
        try {

            Player player = playerDAO.getPlayerById(playerId);
            if (player == null) {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("Player not found")
                        .asRuntimeException());
                return;
            }

            Game game = gameDAO.getGameById(gameId);
            if (game == null) {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("Game not found")
                        .asRuntimeException());
                return;
            }

            if (gameDAO.getGamesByUserId(playerId).contains(gameId)) {
                boolean updated = playerDAO.updatePlayerColor(playerId, pieceColor);

                if (updated) {
                    lobbyEventManager.broadcast(gameId,
                            Catan.GameEvent.newBuilder()
                                    .setType("COLOR_CHANGE")
                                    .setDescription("Player " + playerId + " selected " + pieceColor.name())
                                    .setTimestamp(System.currentTimeMillis())
                                    .build());
                    responseObserver.onNext(Catan.GameResponse.newBuilder()
                            .setSuccess(true)
                            .setMessage("Color updated successfully")
                            .build());
                } else {
                    responseObserver.onNext(Catan.GameResponse.newBuilder()
                            .setSuccess(false)
                            .setMessage("Failed to update color")
                            .build());
                }
                responseObserver.onCompleted();
            } else {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("Player not member in game")
                        .asRuntimeException());
                return;
            }

        }  catch (SQLException e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("DB error: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void startTurn(Catan.GameActionRequest request, StreamObserver<Catan.GameResponse> responseObserver) {
        super.startTurn(request, responseObserver);
    }

    @Override
    public void buildRoad(Catan.BuildRequest request, StreamObserver<Catan.GameResponse> responseObserver) {
        super.buildRoad(request, responseObserver);
    }

    @Override
    public void buildSettlement(Catan.BuildRequest request, StreamObserver<Catan.GameResponse> responseObserver) {
        super.buildSettlement(request, responseObserver);
    }

    @Override
    public void buyDevelopmentCard(Catan.BuyDevCardRequest request, StreamObserver<Catan.GameResponse> responseObserver) {
        super.buyDevelopmentCard(request, responseObserver);
    }

    @Override
    public void discardHalfResources(Catan.DiscardRequest request, StreamObserver<Catan.GameResponse> responseObserver) {
        super.discardHalfResources(request, responseObserver);
    }

    @Override
    public void endTurn(Catan.GameActionRequest request, StreamObserver<Catan.GameResponse> responseObserver) {
        super.endTurn(request, responseObserver);
    }

    @Override
    public void getPlayerInfo(Catan.PlayerInfoRequest request, StreamObserver<Catan.PlayerInfoResponse> responseObserver) {
        super.getPlayerInfo(request, responseObserver);
    }

    @Override
    public void kickPlayer(Catan.KickRequest request, StreamObserver<Catan.GameResponse> responseObserver) {
        super.kickPlayer(request, responseObserver);
    }

    @Override
    public void monopolyCardUse(Catan.MonopolyUseRequest request, StreamObserver<Catan.GameResponse> responseObserver) {
        super.monopolyCardUse(request, responseObserver);
    }

    @Override
    public void moveRobber(Catan.MoveRobberRequest request, StreamObserver<Catan.GameResponse> responseObserver) {
        super.moveRobber(request, responseObserver);
    }

    @Override
    public void playDevelopmentCard(Catan.PlayDevCardRequest request, StreamObserver<Catan.GameResponse> responseObserver) {
        super.playDevelopmentCard(request, responseObserver);
    }

    @Override
    public void promoteToCity(Catan.BuildRequest request, StreamObserver<Catan.GameResponse> responseObserver) {
        super.promoteToCity(request, responseObserver);
    }

    @Override
    public void reportPlayer(Catan.ReportRequest request, StreamObserver<Catan.GameResponse> responseObserver) {
        super.reportPlayer(request, responseObserver);
    }

    @Override
    public void respondToTrade(Catan.TradeResponse request, StreamObserver<Catan.TradeResult> responseObserver) {
        super.respondToTrade(request, responseObserver);
    }

    @Override
    public void roadBuildingCardUse(Catan.RoadBuildingRequest request, StreamObserver<Catan.GameResponse> responseObserver) {
        super.roadBuildingCardUse(request, responseObserver);
    }

    @Override
    public void rollDices(Catan.GameActionRequest request, StreamObserver<Catan.DiceRollResponse> responseObserver) {
        super.rollDices(request, responseObserver);
    }

    @Override
    public void stealCardFromPlayer(Catan.StealRequest request, StreamObserver<Catan.GameResponse> responseObserver) {
        super.stealCardFromPlayer(request, responseObserver);
    }

    @Override
    public void subscribeToGameEvents(Catan.GameSubscribeRequest request, StreamObserver<Catan.GameEvent> responseObserver) {
        super.subscribeToGameEvents(request, responseObserver);
    }

    @Override
    public void tradeRequest(Catan.TradeRequest request, StreamObserver<Catan.TradeResponse> responseObserver) {
        super.tradeRequest(request, responseObserver);
    }

    private void respondConnection(StreamObserver<Catan.ConnectionResponse> observer,
                                   boolean success, String message, String username, List<String> errors, String userId) {

        Catan.ConnectionResponse.Builder builder = Catan.ConnectionResponse.newBuilder()
                .setSuccess(success)
                .setMessage(message);

        if (username != null) builder.setUsername(username);
        if (userId != null) builder.setUserId(userId);
        if (errors != null && !errors.isEmpty()) builder.addAllErrors(errors);

        observer.onNext(builder.build());
        observer.onCompleted();
    }

    private boolean isEmailTaken(String email) {
        try {
            return userDAO.getUserByEmail(email) != null;
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return true; // במקרה של שגיאה, נניח שהאימייל תפוס כדי למנוע הרשמה כפולה
        }
    }

}
