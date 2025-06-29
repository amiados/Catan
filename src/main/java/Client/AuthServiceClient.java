package Client;

import catan.Catan;
import catan.CatanServiceGrpc;
import catan.Catan.*;
import catan.PieceColor;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.stub.StreamObserver;

import java.util.UUID;
import java.util.function.Consumer;

public class AuthServiceClient {
    private final CatanServiceGrpc.CatanServiceFutureStub stub;
    private final CatanServiceGrpc.CatanServiceStub asyncStub;

    public AuthServiceClient(String host, int port) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.stub = CatanServiceGrpc.newFutureStub(channel);
        this.asyncStub = CatanServiceGrpc.newStub(channel);
    }

    // =====================
    // 1. Registration / Login
    // =====================

    public ListenableFuture<ConnectionResponse> register(String username, String email, String password) {
        RegisterRequest request = RegisterRequest.newBuilder()
                .setUsername(username)
                .setEmail(email)
                .setPassword(password)
                .build();
        return stub.register(request);
    }
    public ListenableFuture<ConnectionResponse> login(String email, String password) {
        LoginRequest request = LoginRequest.newBuilder()
                .setEmail(email)
                .setPassword(password)
                .build();
        return stub.login(request);
    }
    public ListenableFuture<ConnectionResponse> sendRegisterOtp(String email) {
        EmailRequest request = EmailRequest.newBuilder().setEmail(email).build();
        return stub.sendRegisterOtp(request);
    }
    public ListenableFuture<ConnectionResponse> sendLoginOtp(String email) {
        EmailRequest request = EmailRequest.newBuilder().setEmail(email).build();
        return stub.sendLoginOtp(request);
    }
    public ListenableFuture<ConnectionResponse> verifyRegisterOtp(String email, String otp) {
        VerifyOtpRequest request = VerifyOtpRequest.newBuilder()
                .setEmail(email)
                .setOtp(otp)
                .build();
        return stub.verifyRegisterOtp(request);
    }
    public ListenableFuture<ConnectionResponse> verifyLoginOtp(String email, String otp) {
        VerifyOtpRequest request = VerifyOtpRequest.newBuilder()
                .setEmail(email)
                .setOtp(otp)
                .build();
        return stub.verifyLoginOtp(request);
    }

    // =====================
    // 2. Group Management
    // =====================

    public ListenableFuture<AllGroupsResponse> getAllGroups(String userId) {
        UserRequest request = UserRequest.newBuilder()
                .setUserId(userId)
                .build();
        return stub.getAllGroups(request);
    }
    public ListenableFuture<GroupResponse> createGroup(String creatorId, String groupName) {
        CreateGroupRequest request = CreateGroupRequest.newBuilder()
                .setCreatorId(creatorId)
                .setGroupName(groupName)
                .build();
        return stub.createGroup(request);
    }
    public ListenableFuture<GroupResponse> inviteToGroup(String groupId, String receiverUserId, String senderUserId) {
        InviteRequest request = InviteRequest.newBuilder()
                .setInviteId(UUID.randomUUID().toString()) // פונקציה פנימית שאתה צריך להוסיף
                .setGroupId(groupId)
                .setSenderId(senderUserId)
                .setReceiverId(receiverUserId)
                .setTimestamp(System.currentTimeMillis())
                .build();
        return stub.inviteToGroup(request);
    }
    public ListenableFuture<GroupResponse> respondToInvite(String inviteId, String chatId, String senderId, String receiverId, InviteResponseStatus status) {
        InviteResponse response = InviteResponse.newBuilder()
                .setInviteId(inviteId)
                .setGroupId(chatId)
                .setSenderId(senderId)
                .setReceiverId(receiverId)
                .setStatus(status)
                .build();
        return stub.respondToGroupInvitation(response);
    }
    public ListenableFuture<InviteListResponse> getUserInvites(String userId) {
        UserRequest request = UserRequest.newBuilder()
                .setUserId(userId)
                .build();
        return stub.getUserInvites(request);
    }
    public ListenableFuture<GameListResponse> listGroupGames(String groupId) {
        GroupRequest req = GroupRequest.newBuilder()
                .setGroupId(groupId)
                .build();
        return stub.listGroupGames(req);
    }
    public ListenableFuture<GroupInfo> getGroupInfo(String groupId) {
        Catan.GroupRequest request = Catan.GroupRequest.newBuilder()
                .setGroupId(groupId)
                .build();
        return stub.getGroupInfo(request);
    }

    // =====================
    // 3. Game Management
    // =====================

    // ניהול משחקים בקבוצה
    public ListenableFuture<GameResponse> createGame(String groupId, String gameId, String userId) {
        return stub.createGame(buildGameAction(groupId, gameId, userId));
    }
    public ListenableFuture<GameResponse> startGame(String groupId, String gameId, String userId) {
        return stub.startGame(buildGameAction(groupId, gameId, userId));
    }
    public ListenableFuture<GameResponse> endGame(String groupId, String gameId, String userId) {
        return stub.endGame(buildGameAction(groupId, gameId, userId));
    }
    public ListenableFuture<GameResponse> leaveGame(String groupId, String gameId, String userId) {
        return stub.leaveGame(buildGameAction(groupId, gameId, userId));
    }
    public ListenableFuture<GameResponse> updateColor(String playerId, PieceColor pieceColor) {
        Catan.UpdateColorRequest request = UpdateColorRequest.newBuilder()
                .setPlayerId(playerId)
                .setColor(Catan.Color.valueOf(pieceColor.name()))
                .build();
        return stub.updatePlayerColor(request);
    }
    public ListenableFuture<GamePlayersResponse> getGamePlayers(String gameId, String groupId, String userId) {
        Catan.GameActionRequest request = Catan.GameActionRequest.newBuilder()
                .setGameId(gameId)
                .setGroupId(groupId)
                .setUserId(userId)
                .setTimestamp(System.currentTimeMillis())
                .build();
        return stub.getGamePlayers(request);
    }
    public ListenableFuture<GameResponse> updateColor(String playerId, String gameId, Catan.Color color) {
        Catan.UpdateColorRequest request = Catan.UpdateColorRequest.newBuilder()
                .setPlayerId(playerId)
                .setGameId(gameId)
                .setColor(color)
                .build();

        return stub.updatePlayerColor(request);
    }
    private GameActionRequest buildGameAction(String groupId, String gameId, String userId) {
        return GameActionRequest.newBuilder()
                .setGameId(gameId)
                .setGroupId(groupId)
                .setUserId(userId)
                .setTimestamp(System.currentTimeMillis())
                .build();
    }

    // =====================
    // 4. Chat & Subscriptions
    // =====================

    // -- Group Chat --
    public ListenableFuture<ACK> sendGroupMessage(String groupId, String fromUserId, byte[] content) {
        GroupChatMessage msg = GroupChatMessage.newBuilder()
                .setGroupId(groupId)
                .setFromUserId(fromUserId)
                .setContent(ByteString.copyFrom(content))
                .setTimestamp(System.currentTimeMillis())
                .build();
        return stub.sendGroupMessage(msg);
    }
    public void subscribeGroupMessages(String groupId, String userId, StreamObserver<GroupChatMessage> obs) {
        GroupSubscribeRequest req = GroupSubscribeRequest.newBuilder()
                .setGroupId(groupId)
                .setUserId(userId)
                .build();
        asyncStub.subscribeGroupMessages(req, obs);
    }

    // -- Game Chat --
    public ListenableFuture<ACK> sendGameMessage(String gameId, String fromPlayerId, byte[] content) {
        GameChatMessage msg = GameChatMessage.newBuilder()
                .setGameId(gameId)
                .setFromPlayerId(fromPlayerId)
                .setContent(ByteString.copyFrom(content))
                .setTimestamp(System.currentTimeMillis())
                .build();
        return stub.sendGameMessage(msg);
    }
    public void subscribeGameMessages(String gameId, String playerId, StreamObserver<GameChatMessage> obs) {
        GameSubscribeRequest req = GameSubscribeRequest.newBuilder()
                .setGameId(gameId)
                .setPlayerId(playerId)
                .build();
        asyncStub.subscribeGameMessages(req, obs);
    }

    // -- Game Events Stream --

    public void subscribeToGameEvents(String gameId, String playerId, Consumer<Catan.GameEvent> handler) {
        Catan.GameSubscribeRequest request = Catan.GameSubscribeRequest.newBuilder()
                .setGameId(gameId)
                .setPlayerId(playerId)
                .build();

        asyncStub.subscribeToGameEvents(request, new StreamObserver<Catan.GameEvent>() {
            @Override
            public void onNext(Catan.GameEvent event) {
                handler.accept(event);
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Game event stream error: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.println("Game event stream completed.");
            }
        });
    }
}
