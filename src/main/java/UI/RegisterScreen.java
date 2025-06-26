package UI;

import Client.AuthServiceClient;
import catan.Catan;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.Objects;
import java.util.concurrent.Executors;
import com.google.common.util.concurrent.ListenableFuture;

public class RegisterScreen extends Application {

    private final AuthServiceClient authClient;
    private AudioClip clickSound;
    private boolean isRegistering = false;

    public RegisterScreen(AuthServiceClient authClient) {
        this.authClient = authClient;
    }

    @Override
    public void start(Stage primaryStage) {
        VBox root = new VBox(20);
        root.setPadding(new Insets(40));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #f7e2c3, #d3b18d);");

        clickSound = new AudioClip(
                Objects.requireNonNull(getClass().getResource("/Utils/mouse_click.mp3")).toExternalForm()
        );
        clickSound.setVolume(0.5);

        Label title = new Label("Register to Catan Game");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 28));

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        styleTextField(usernameField);

        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        styleTextField(emailField);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        styleTextField(passwordField);

        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm Password");
        styleTextField(confirmPasswordField);

        Label errorLabel = new Label();
        errorLabel.setTextFill(Color.RED);
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(300);

        ProgressIndicator loadingSpinner = new ProgressIndicator();
        loadingSpinner.setVisible(false);
        loadingSpinner.setPrefSize(30, 30);

        Button registerBtn = new Button("Register");
        styleButton(registerBtn);
        registerBtn.setOnAction(e -> {
            if (isRegistering) return;
            isRegistering = true;
            clickSound.play();

            registerBtn.setText("Registering...");
            registerBtn.setDisable(true);
            loadingSpinner.setVisible(true);
            errorLabel.setText("");

            String username = usernameField.getText().trim();
            String email = emailField.getText().trim();
            String password = passwordField.getText();
            String confirmPassword = confirmPasswordField.getText();

            Runnable reset = () -> {
                isRegistering = false;
                registerBtn.setDisable(false);
                registerBtn.setText("Register");
                loadingSpinner.setVisible(false);
            };

            if (!isValidEmail(email)) {
                showError(errorLabel, "Invalid email format");
                reset.run();
            } else if (!isValidPassword(password)) {
                showError(errorLabel, "Password must be at least 8 characters,\nincluding upper, lower, digit, and symbol");
                reset.run();
            } else if (!password.equals(confirmPassword)) {
                showError(errorLabel, "Passwords do not match");
                reset.run();
            } else if (!isValidUsername(username)) {
                showError(errorLabel, "Username must be at least 6 characters");
                reset.run();
            } else {
                ListenableFuture<Catan.ConnectionResponse> future = authClient.register(username, email, password);
                future.addListener(() -> {
                    try {
                        Catan.ConnectionResponse response = future.get();
                        Platform.runLater(() -> {
                            if (response.getSuccess()) {
                                try {
                                    new OTPVerificationScreen(authClient, email, true).start(new Stage());
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
                            showError(errorLabel, "Server error during registration");
                            reset.run();
                        });
                    }
                }, Executors.newSingleThreadExecutor());
            }
        });

        Hyperlink loginLink = new Hyperlink("Already have an account? Login");
        loginLink.setOnAction(e -> {
            try {
                clickSound.play();
                new LoginScreen(authClient).start(new Stage());
                primaryStage.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        root.getChildren().addAll(
                title,
                usernameField,
                emailField,
                passwordField,
                confirmPasswordField,
                registerBtn,
                loadingSpinner,
                errorLabel,
                loginLink
        );

        Scene scene = new Scene(root, 500, 520);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) registerBtn.fire();
        });

        primaryStage.setTitle("Register");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,6}$");
    }
    private boolean isValidUsername(String username) {
        return username != null && username.length() >= 6;
    }
    private boolean isValidPassword(String password) {
        if (password == null) return false;
        return password.length() >= 8 &&
                password.matches(".*[A-Z].*") &&
                password.matches(".*[a-z].*") &&
                password.matches(".*\\d.*") &&
                password.matches(".*[^a-zA-Z0-9].*");
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
