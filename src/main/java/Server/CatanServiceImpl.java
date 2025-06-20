package Server;

import Model.dao.*;
import Model.obj.Group;
import Model.obj.User;
import Security.PasswordHasher;
import Utils.EmailSender;
import Utils.OTPManager;
import Utils.OTP_Entry;
import Utils.ValidationResult;
import catan.Catan;
import catan.CatanServiceGrpc;
import com.google.common.cache.Cache;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CatanServiceImpl extends CatanServiceGrpc.CatanServiceImplBase {

    private final Cache<String, OTP_Entry> otpCache;
    private final Cache<String, User> pendingRegistrations;
    private final Cache<String, User> pendingUsers;

    private final UserDAO userDAO;
    private final MessageDAO messageDAO;
    private final InviteDAO inviteDAO;
    private final GameDAO gameDAO;
    private final PlayerDAO playerDAO;
    private final GroupDAO groupDAO;
    private final GroupMemberDAO groupMemberDAO;

    private final Map<UUID, Map<UUID, StreamObserver<Catan.Message>>> subscribers = new ConcurrentHashMap<>();

    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final int LOCK_DURATION_MINUTES = 10;
    private OTPManager otpManager = new OTPManager();

    public CatanServiceImpl(Cache<String, OTP_Entry> otpCache, Cache<String, User> pendingRegistrations, Cache<String, User> pendingUsers,
                            UserDAO userDAO, MessageDAO messageDAO, InviteDAO inviteDAO, GameDAO gameDAO, PlayerDAO playerDAO, GroupDAO groupDAO, GroupMemberDAO groupMemberDAO) {
        this.otpCache = otpCache;
        this.pendingRegistrations = pendingRegistrations;
        this.pendingUsers = pendingUsers;
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
            pendingRegistrations.put(email, user);

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
        verifyOtp(request, responseObserver, pendingUsers, false);
    }

    private void verifyOtp(Catan.VerifyOtpRequest request, StreamObserver<Catan.ConnectionResponse>responseObserver, Cache <String, User> userCache, boolean isRegistration) {
        String email = request.getEmail();
        String otp = request.getOtp();

        OTP_Entry entry = otpCache.getIfPresent(email);
        User user = userCache.getIfPresent(email);

        if (entry == null || user == null || !entry.isValid(otp)) {
            respondConnection(responseObserver, false,
                    "Invalid or expired OTP",
                    user.getUsername(),
                    null,
                    user.getId().toString());
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
                pendingUsers.put(email, existingUser); // שומר את המשתמש בזיכרון זמני
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
                String creatorUsername = userDAO.getUserById(group.getCreatorId()).getUsername();

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
        if (groupName == null || groupName.isBlank()) {
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
                groupMemberDAO.addMemberToGroup(groupId, creatorId, "ADMIN");

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
    public void inviteToGroup(Catan.InviteToGroupRequest request, StreamObserver<Catan.GroupResponse> responseObserver) {
//        UUID groupId;
//        try {
//            groupId = UUID.fromString(request.getGroupId());
//        }
    }
    @Override
    public void getGamePlayers(Catan.GameActionRequest request, StreamObserver<Catan.GamePlayersResponse> responseObserver) {
        super.getGamePlayers(request, responseObserver);
    }

    @Override
    public void updatePlayerColor(Catan.UpdateColorRequest request, StreamObserver<Catan.GameResponse> responseObserver) {
        super.updatePlayerColor(request, responseObserver);
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
    public void endGame(Catan.GameActionRequest request, StreamObserver<Catan.GameResponse> responseObserver) {
        super.endGame(request, responseObserver);
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
    public void leaveGame(Catan.GameActionRequest request, StreamObserver<Catan.GameResponse> responseObserver) {
        super.leaveGame(request, responseObserver);
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
    public void respondToGroupInvitation(Catan.GroupInviteResponse request, StreamObserver<Catan.GroupResponse> responseObserver) {
        super.respondToGroupInvitation(request, responseObserver);
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
    public void sendMessage(Catan.Message request, StreamObserver<Catan.ACK> responseObserver) {
        super.sendMessage(request, responseObserver);
    }

    @Override
    public void startGame(Catan.GameActionRequest request, StreamObserver<Catan.GameResponse> responseObserver) {
        super.startGame(request, responseObserver);
    }

    @Override
    public void stealCardFromPlayer(Catan.StealRequest request, StreamObserver<Catan.GameResponse> responseObserver) {
        super.stealCardFromPlayer(request, responseObserver);
    }

    @Override
    public void subscribeMessages(Catan.ChatSubscribeRequest request, StreamObserver<Catan.Message> responseObserver) {
        super.subscribeMessages(request, responseObserver);
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
