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
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.Objects;
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
    private ProgressIndicator loadingSpinner;
    private ProgressIndicator resendSpinner;

    private Stage currentStage;

    private AudioClip clickSound;

    private boolean isVerifyingOtp = false;
    private boolean isResendingOtp = false;

    public OTPVerificationScreen(AuthServiceClient authClient, String email, boolean isRegisterMode) {
        this.authClient = authClient;
        this.email = email;
        this.isRegisterMode = isRegisterMode;
    }

    @Override
    public void start(Stage stage) {
        this.currentStage = stage;

        clickSound = new AudioClip(
                Objects.requireNonNull(getClass().getResource("/Utils/mouse_click.mp3")).toExternalForm()
        );
        clickSound.setVolume(0.5);

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

        loadingSpinner = new ProgressIndicator();
        loadingSpinner.setVisible(false);
        loadingSpinner.setPrefSize(30, 30);

        resendSpinner = new ProgressIndicator();
        resendSpinner.setVisible(false);
        resendSpinner.setPrefSize(25, 25);

        verifyBtn = new Button("Verify OTP");
        styleButton(verifyBtn);
        verifyBtn.setOnAction(e -> {
            clickSound.play();
            verifyOtp();
        });

        resendBtn = new Button("Send OTP Again");
        styleButton(resendBtn);
        resendBtn.setOnAction(e -> {
            clickSound.play();
            resendOtp();
        });

        messageLabel = new Label();
        messageLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        timerLabel = new Label();
        timerLabel.setFont(Font.font("Arial", 14));

        root.getChildren().addAll(
                title,
                otpField,
                verifyBtn,
                loadingSpinner,
                resendBtn,
                resendSpinner,
                messageLabel,
                timerLabel
        );

        Scene scene = new Scene(root, 400, 350);
        stage.setTitle("OTP Verification");
        stage.setScene(scene);
        stage.show();

        startCountdown();
    }

    private void verifyOtp() {
        if (isVerifyingOtp) return;
        isVerifyingOtp = true;

        String otp = otpField.getText().trim();
        if (otp.isEmpty()) {
            showMessage("Please enter OTP", Color.RED);
            animateShake(otpField);
            isVerifyingOtp = false;
            return;
        }

        verifyBtn.setText("Verifying...");
        verifyBtn.setDisable(true);
        loadingSpinner.setVisible(true);

        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                Catan.ConnectionResponse response = (isRegisterMode ?
                        authClient.verifyRegisterOtp(email, otp) :
                        authClient.verifyLoginOtp(email, otp)).get();

                if (response.getSuccess()) {
                    Platform.runLater(() -> {
                        showMessage("OTP Verified!", Color.GREEN);
                        if (countdown != null) countdown.stop();
                        try {
                            new HomePage(response.getUserId(), authClient).start(new Stage());
                            currentStage.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                            showMessage("Failed to load Home Page", Color.RED);
                            resetVerifyButton();
                        }
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
                        resetVerifyButton();
                    });
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    showMessage("Server error during OTP verification", Color.RED);
                    resetVerifyButton();
                });
            }
        });
    }

    private void resetVerifyButton() {
        isVerifyingOtp = false;
        verifyBtn.setDisable(false);
        verifyBtn.setText("Verify OTP");
        loadingSpinner.setVisible(false);
    }
    private void resetResendButton() {
        isResendingOtp = false;
        resendBtn.setDisable(false);
        resendBtn.setText("Send OTP Again");
        resendSpinner.setVisible(false);
    }

    private void resendOtp() {
        if (isResendingOtp) return;
        if (resendAttempts >= 3) {
            showMessage("Maximum resend attempts reached. Wait 5 minutes.", Color.RED);
            resendBtn.setDisable(true);
            return;
        }

        isResendingOtp = true;
        resendAttempts++;

        resendBtn.setText("Sending...");
        resendBtn.setDisable(true);
        resendSpinner.setVisible(true);

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
                    resetResendButton();
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showMessage("Server error when sending OTP", Color.RED);
                    resetResendButton();
                });
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
