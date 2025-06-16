package UI;

import Client.AuthServiceClient;
import javafx.application.Application;
import javafx.stage.Stage;

public class HomePage extends Application {

    private final AuthServiceClient authClient;

    public HomePage(AuthServiceClient authClient) {
        this.authClient = authClient;
    }

    @Override
    public void start(Stage stage) throws Exception {

    }
}
