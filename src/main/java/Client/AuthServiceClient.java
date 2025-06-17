package Client;

import catan.CatanServiceGrpc;
import catan.Catan.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import com.google.common.util.concurrent.ListenableFuture;

public class AuthServiceClient {
    private final CatanServiceGrpc.CatanServiceFutureStub stub;

    public AuthServiceClient(String host, int port) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.stub = CatanServiceGrpc.newFutureStub(channel);
    }

    // הרשמה וכניסה
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

    public ListenableFuture<AllGroupsResponse> getAllGroups(String userId) {
        UserRequest request = UserRequest.newBuilder()
                .setUserId(userId)
                .build();
        return stub.getAllGroups(request);
    }

    // ניהול קבוצות
    public ListenableFuture<GroupResponse> createGroup(String creatorId, String groupName) {
        CreateGroupRequest request = CreateGroupRequest.newBuilder()
                .setCreatorId(creatorId)
                .setGroupName(groupName)
                .build();
        return stub.createGroup(request);
    }

    public ListenableFuture<GroupResponse> inviteToGroup(String groupId, String invitedUserId) {
        InviteToGroupRequest request = InviteToGroupRequest.newBuilder()
                .setGroupId(groupId)
                .setInvitedUserId(invitedUserId)
                .build();
        return stub.inviteToGroup(request);
    }

    public ListenableFuture<GroupResponse> respondToGroupInvitation(String groupId, String userId, boolean accepted) {
        GroupInviteResponse request = GroupInviteResponse.newBuilder()
                .setGroupId(groupId)
                .setUserId(userId)
                .setAccepted(accepted)
                .build();
        return stub.respondToGroupInvitation(request);
    }

    // ניהול משחקים בקבוצה
    public ListenableFuture<GameResponse> startGame(String gameId, String userId) {
        return stub.startGame(buildGameAction(gameId, userId));
    }

    public ListenableFuture<GameResponse> endGame(String gameId, String userId) {
        return stub.endGame(buildGameAction(gameId, userId));
    }

    public ListenableFuture<GameResponse> leaveGame(String gameId, String userId) {
        return stub.leaveGame(buildGameAction(gameId, userId));
    }

    // פעולת עזר פנימית
    private GameActionRequest buildGameAction(String gameId, String userId) {
        return GameActionRequest.newBuilder()
                .setGameId(gameId)
                .setUserId(userId)
                .build();
    }

}
