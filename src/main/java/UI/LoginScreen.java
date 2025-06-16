package UI;

import Client.AuthServiceClient;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.effect.DropShadow;

public class LoginScreen extends Application {

    private final AuthServiceClient authClient;

    public LoginScreen(AuthServiceClient authClient) {
        this.authClient = authClient;
    }

    @Override
    public void start(Stage primaryStage) {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #f7e2c3, #d3b18d);");

        Label title = new Label("Login to Catan");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 28));

        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        styleTextField(emailField);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        styleTextField(passwordField);

        Label errorLabel = new Label();
        errorLabel.setTextFill(Color.RED);

        Button loginBtn = new Button("Login");
        styleButton(loginBtn);

        loginBtn.setOnAction(e -> {
            String email = emailField.getText();
            String password = passwordField.getText();

            if (!isValidEmail(email)) {
                errorLabel.setText("Invalid email format");
            } else if (password.length() < 6) {
                errorLabel.setText("Password must be at least 6 characters");
            } else {
                errorLabel.setText("");
                // Attempt login logic
                // stage.setScene(MainMenuScreen.createScene()); // Example next scene
            }
        });

        Hyperlink switchToRegister = new Hyperlink("Don't have an account? Register here");
        switchToRegister.setOnAction(e -> {
            try {
                new RegisterScreen(authClient).start(new Stage());
                primaryStage.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        root.getChildren().addAll(title, emailField, passwordField, loginBtn, errorLabel, switchToRegister);

        Scene scene = new Scene(root, 500, 450);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) loginBtn.fire();
        });

        primaryStage.setScene(scene);
        primaryStage.setTitle("Catan - Login");
        primaryStage.show();
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,6}$");
    }

    private void styleButton(Button button) {
        button.setStyle("-fx-background-color: #8b5e3c; -fx-text-fill: white; -fx-font-size: 16px; -fx-background-radius: 10;");
        button.setPrefWidth(200);
        button.setEffect(new DropShadow());
        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: #a56c46; -fx-text-fill: white; -fx-font-size: 16px; -fx-background-radius: 10;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-background-color: #8b5e3c; -fx-text-fill: white; -fx-font-size: 16px; -fx-background-radius: 10;"));
    }

    private void styleTextField(TextField textField) {
        textField.setPrefWidth(300);
        textField.setFont(Font.font("Arial", 14));
        textField.setStyle("-fx-background-radius: 8; -fx-padding: 8px;");
        textField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal)
                textField.setStyle("-fx-background-radius: 8; -fx-border-color: #a56c46; -fx-border-width: 2px;");
            else
                textField.setStyle("-fx-background-radius: 8;");
        });
    }

}
