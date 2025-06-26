package UI;

import Client.AuthServiceClient;
import catan.Catan;
import com.google.common.util.concurrent.ListenableFuture;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.effect.DropShadow;

import java.util.Objects;
import java.util.concurrent.Executors;

public class LoginScreen extends Application {

    private final AuthServiceClient authClient;
    private AudioClip clickSound;
    private boolean isLoggingIn = false;

    public LoginScreen(AuthServiceClient authClient) {
        this.authClient = authClient;
    }

    @Override
    public void start(Stage primaryStage) {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #f7e2c3, #d3b18d);");

        clickSound = new AudioClip(
                Objects.requireNonNull(getClass().getResource("/Utils/mouse_click.mp3")).toExternalForm()
        );
        clickSound.setVolume(0.5);

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

        ProgressIndicator loadingSpinner = new ProgressIndicator();
        loadingSpinner.setVisible(false);
        loadingSpinner.setPrefSize(30, 30);

        Button loginBtn = new Button("Login");
        styleButton(loginBtn);

        loginBtn.setOnAction(e -> {
            if (isLoggingIn) return;
            isLoggingIn = true;
            clickSound.play();

            loginBtn.setText("Logging in...");
            loginBtn.setDisable(true);
            loadingSpinner.setVisible(true);
            errorLabel.setText("");

            String email = emailField.getText();
            String password = passwordField.getText();

            Runnable reset = () -> {
                isLoggingIn = false;
                loginBtn.setDisable(false);
                loginBtn.setText("Login");
                loadingSpinner.setVisible(false);
            };

            if (!isValidEmail(email)) {
                errorLabel.setText("Invalid email format");
                reset.run();
            } else if (password.length() < 6) {
                errorLabel.setText("Password must be at least 6 characters");
                reset.run();
            } else {
                ListenableFuture<Catan.ConnectionResponse> future = authClient.login(email, password);
                future.addListener(() -> {
                    try {
                        Catan.ConnectionResponse response = future.get();
                        Platform.runLater(() -> {
                            if (response.getSuccess()) {
                                try {
                                    new OTPVerificationScreen(authClient, email, false).start(new Stage());
                                    primaryStage.close();
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                    showError(errorLabel, "Failed to load OTP screen");
                                    reset.run();
                                }
                            } else {
                                showError(errorLabel, String.join("\n", response.getErrorsList()));
                                reset.run();
                            }
                        });
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        Platform.runLater(() -> {
                            showError(errorLabel, "Server error during login");
                            reset.run();
                        });
                    }
                }, Executors.newSingleThreadExecutor());
            }
        });

        Hyperlink switchToRegister = new Hyperlink("Don't have an account? Register here");
        switchToRegister.setOnAction(e -> {
            try {
                clickSound.play();
                new RegisterScreen(authClient).start(new Stage());
                primaryStage.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        root.getChildren().addAll(
                title,
                emailField,
                passwordField,
                loginBtn,
                loadingSpinner,
                errorLabel,
                switchToRegister
        );

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

    private void showError(Label label, String message) {
        label.setText(message);
    }

}
