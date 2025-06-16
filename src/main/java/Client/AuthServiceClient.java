package Client;

import catan.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import com.google.common.util.concurrent.ListenableFuture;

public class AuthServiceClient {
    private final catanGrpc.catanFutureStub stub;

    public AuthServiceClient(String host, int port) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.stub = catanGrpc.newFutureStub(channel);
    }

    public ListenableFuture<Catan.ConnectionResponse> login(String email, String password) {
        Catan.LoginRequest request = Catan.LoginRequest.newBuilder()
                .setEmail(email)
                .setPassword(password)
                .build();
        return stub.login(request);
    }

    public ListenableFuture<Catan.ConnectionResponse> register(String username, String email, String password) {
        Catan.RegisterRequest request = Catan.RegisterRequest.newBuilder()
                .setUsername(username)
                .setEmail(email)
                .setPassword(password)
                .build();
        return stub.register(request);
    }

    public ListenableFuture<Catan.ConnectionResponse> sendRegisterOtp(String email) {
        Catan.EmailRequest request = Catan.EmailRequest.newBuilder()
                .setEmail(email)
                .build();
        return stub.sendRegisterOtp(request);
    }

    public ListenableFuture<Catan.ConnectionResponse> sendLoginOtp(String email) {
        Catan.EmailRequest request = Catan.EmailRequest.newBuilder()
                .setEmail(email)
                .build();
        return stub.sendLoginOtp(request);
    }

    public ListenableFuture<Catan.ConnectionResponse> verifyRegisterOtp(String email, String otp) {
        Catan.VerifyOtpRequest request = Catan.VerifyOtpRequest.newBuilder()
                .setEmail(email)
                .setOtp(otp)
                .build();
        return stub.verifyRegisterOtp(request);
    }

    public ListenableFuture<Catan.ConnectionResponse> verifyLoginOtp(String email, String otp) {
        Catan.VerifyOtpRequest request = Catan.VerifyOtpRequest.newBuilder()
                .setEmail(email)
                .setOtp(otp)
                .build();
        return stub.verifyLoginOtp(request);
    }

}


