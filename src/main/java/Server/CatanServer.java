package Server;

import Model.dao.*;
import Model.obj.User;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.util.concurrent.TimeUnit;

public class CatanServer {
    public static void main(String[] args) {
        try {
            // יצירת קאש עם תפוגה אחרי 5 דקות
            Cache<String, Utils.OTP_Entry> otpCache = CacheBuilder.newBuilder()
                    .expireAfterWrite(5, TimeUnit.MINUTES)
                    .build();

            Cache<String, User> pendingRegistrations = CacheBuilder.newBuilder()
                    .expireAfterWrite(10, TimeUnit.MINUTES)
                    .build();

            Cache<String, User> pendingUsers = CacheBuilder.newBuilder()
                    .expireAfterWrite(10, TimeUnit.MINUTES)
                    .build();

            UserDAO userDAO = new UserDAO();
            MessageDAO messageDAO = new MessageDAO();
            InviteDAO inviteDAO = new InviteDAO();
            GameDAO gameDAO = new GameDAO();
            PlayerDAO playerDAO = new PlayerDAO();
            GroupDAO groupDAO = new GroupDAO();
            GroupMemberDAO groupMemberDAO = new GroupMemberDAO();

            CatanServiceImpl service = new CatanServiceImpl(
                    otpCache,
                    pendingRegistrations,
                    pendingUsers,
                    userDAO,
                    messageDAO,
                    inviteDAO,
                    gameDAO,
                    playerDAO,
                    groupDAO,
                    groupMemberDAO
            );

            // יצירת שרת על פורט 9090
            Server server = ServerBuilder
                    .forPort(9090)
                    .addService(service) // ודא שזו המחלקה שלך שמיישמת את השירות
                    .build();

            server.start();
            System.out.println("Catan gRPC Server started on port 9090");

            // מונע מהתוכנית להסתיים כל עוד השרת רץ
            server.awaitTermination();
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
