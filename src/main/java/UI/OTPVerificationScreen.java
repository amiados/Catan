package UI;

import Client.AuthServiceClient;
import catan.Catan;
import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.concurrent.Executors;

public class OTPVerificationScreen extends Application {

    private final AuthServiceClient authClient;
    private final String email;
    private final boolean isRegisterMode;
    private int remainingSeconds = 300;
    private int resendAttempts = 0;
    private int failedAttempts = 0;

    private Label messageLabel;
    private Label timerLabel;
    private TextField otpField;
    private Button verifyBtn;
    private Button resendBtn;
    private Timeline countdown;

    public OTPVerificationScreen(AuthServiceClient authClient, String email, boolean isRegisterMode) {
        this.authClient = authClient;
        this.email = email;
        this.isRegisterMode = isRegisterMode;
    }

    @Override
    public void start(Stage stage) {
        VBox root = new VBox(20);
        root.setPadding(new Insets(40));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #f7e2c3, #d3b18d);");

        Label title = new Label("Verify OTP");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));

        otpField = new TextField();
        otpField.setPromptText("Enter OTP");
        otpField.setMaxWidth(200);
        otpField.setFont(Font.font("Arial", 16));

        verifyBtn = new Button("Verify OTP");
        styleButton(verifyBtn);
        verifyBtn.setOnAction(e -> verifyOtp());

        resendBtn = new Button("Send OTP Again");
        styleButton(resendBtn);
        resendBtn.setOnAction(e -> resendOtp());

        messageLabel = new Label();
        messageLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        timerLabel = new Label();
        timerLabel.setFont(Font.font("Arial", 14));

        root.getChildren().addAll(title, otpField, verifyBtn, resendBtn, messageLabel, timerLabel);

        Scene scene = new Scene(root, 400, 350);
        stage.setTitle("OTP Verification");
        stage.setScene(scene);
        stage.show();

        startCountdown();
        sendOtp();
    }

    private void verifyOtp() {
        String otp = otpField.getText().trim();
        if (otp.isEmpty()) {
            showMessage("Please enter OTP", Color.RED);
            animateShake(otpField);
            return;
        }

        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                Catan.ConnectionResponse response = (isRegisterMode ?
                        authClient.verifyRegisterOtp(email, otp) :
                        authClient.verifyLoginOtp(email, otp)).get();

                if (response.getSuccess()) {
                    Platform.runLater(() -> {
                        showMessage("OTP Verified!", Color.GREEN);
                        if (countdown != null) countdown.stop();
                        // TODO: move to main screen or game lobby
                    });
                } else {
                    failedAttempts++;
                    Platform.runLater(() -> {
                        animateShake(otpField);
                        showMessage("Invalid OTP: " + response.getMessage(), Color.RED);
                        if (failedAttempts >= 3) {
                            verifyBtn.setDisable(true);
                            showMessage("Too many failed attempts. Please try again later.", Color.RED);
                        }
                    });
                }
            } catch (Exception ex) {
                Platform.runLater(() -> showMessage("Server error during OTP verification", Color.RED));
                ex.printStackTrace();
            }
        });
    }

    private void resendOtp() {
        if (resendAttempts >= 3) {
            showMessage("Maximum resend attempts reached. Wait 5 minutes.", Color.RED);
            resendBtn.setDisable(true);
            return;
        }

        resendAttempts++;
        sendOtp();
    }

    private void sendOtp() {
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                Catan.ConnectionResponse response = isRegisterMode ?
                        authClient.sendRegisterOtp(email).get() :
                        authClient.sendLoginOtp(email).get();
                Platform.runLater(() -> {
                    if (response.getSuccess()) {
                        showMessage("OTP sent to your email.", Color.BLUE);
                        startCountdown();
                    } else {
                        showMessage("Failed to send OTP: " + response.getMessage(), Color.RED);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showMessage("Server error when sending OTP", Color.RED));
                e.printStackTrace();
            }
        });
    }

    private void startCountdown() {
        if (countdown != null) countdown.stop();
        remainingSeconds = 300;
        countdown = new Timeline(new KeyFrame(Duration.seconds(1), ev -> {
            remainingSeconds--;
            int minutes = remainingSeconds / 60;
            int seconds = remainingSeconds % 60;
            timerLabel.setText(String.format("Time left: %02d:%02d", minutes, seconds));
            if (remainingSeconds <= 0) {
                countdown.stop();
                showMessage("OTP expired. Please request again.", Color.RED);
                resendBtn.setDisable(false);
            }
        }));
        countdown.setCycleCount(300);
        countdown.play();
    }

    private void showMessage(String msg, Color color) {
        messageLabel.setText(msg);
        messageLabel.setTextFill(color);
    }

    private void styleButton(Button button) {
        button.setStyle("-fx-background-color: #8b5e3c; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 8;");
        button.setPrefWidth(200);
        button.setEffect(new DropShadow());
        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: #a56c46; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 8;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-background-color: #8b5e3c; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 8;"));
    }

    private void animateShake(TextField field) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(100), field);
        tt.setFromX(0);
        tt.setByX(10);
        tt.setCycleCount(4);
        tt.setAutoReverse(true);
        tt.play();
    }
}
