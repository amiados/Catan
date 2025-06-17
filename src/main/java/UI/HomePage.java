package UI;

import Client.AuthServiceClient;
import catan.Catan;
import com.google.common.util.concurrent.ListenableFuture;
import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.media.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.util.List;

public class HomePage extends Application {

    private final AuthServiceClient authClient;
    private ListView<String> roomsList;
    private Timeline roomRefresher;
    private MediaPlayer mediaPlayer;
    private final String userId;

    public HomePage(String userId, AuthServiceClient authClient) {
        this.userId = userId;
        this.authClient = authClient;
    }

    @Override
    public void start(Stage primaryStage) {
        StackPane root = new StackPane();

        // רקע מים מונפש (GIF או MP4)
        ImageView background = new ImageView(new Image(getClass().getResource("/Utils/water_background.gif").toExternalForm()));
        background.setFitWidth(800);
        background.setFitHeight(600);
        background.setPreserveRatio(false);

        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);

        // כותרת
        Label userLabel = new Label("Welcome to Catan!");
        userLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 28));
        userLabel.setTextFill(Color.WHITE);
        addGlow(userLabel);

        // כפתור יצירת חדר
        Button createRoomBtn = styledButton("➕ Create Room");
        createRoomBtn.setOnAction(e -> {
            // TODO: פתח חלון ליצירת חדר
        });

        // כפתור להצגת הזמנות
        Button invitesBtn = styledButton("📨 Invitations");
        invitesBtn.setOnAction(e -> {
            // TODO: הצג רשימת הזמנות
        });

        // רשימת חדרים קיימים
        roomsList = new ListView<>();
        roomsList.setPrefSize(300, 200);
        roomsList.setStyle("-fx-background-radius: 8; -fx-font-size: 14px;");
        roomsList.setPlaceholder(new Label("No rooms available"));
        roomsList.setOnMouseClicked(event -> {
            String selected = roomsList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                // TODO: התחברות לחדר
            }
        });

        // כפתור להשתקת מוזיקה
        Button muteBtn = styledButton("🔇 Mute");
        muteBtn.setOnAction(e -> {
            if (mediaPlayer != null) {
                mediaPlayer.setMute(!mediaPlayer.isMute());
                muteBtn.setText(mediaPlayer.isMute() ? "🔊 Unmute" : "🔇 Mute");
            }
        });

        content.getChildren().addAll(userLabel, createRoomBtn, invitesBtn, new Label("Available Rooms:"), roomsList, muteBtn);
        root.getChildren().addAll(background, content);

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("Catan - Home");
        primaryStage.setScene(scene);
        primaryStage.show();

        playMusic();
        refreshRoomListPeriodically();
    }

    private void refreshRoomListPeriodically() {
        roomRefresher = new Timeline(new KeyFrame(Duration.seconds(5), ev -> {
            try {
                ListenableFuture<Catan.AllGroupsResponse> future = authClient.getAllGroups(userId);
                future.addListener(() -> {
                    try {
                        Catan.AllGroupsResponse response = future.get();
                        List<String> roomNames = response.getGroupsList().stream()
                                .map(group -> group.getGroupName() + " (by " + group.getCreatorUsername() + ")")
                                .toList();
                        Platform.runLater(() -> roomsList.getItems().setAll(roomNames));
                    } catch (Exception ex) {
                        Platform.runLater(() -> roomsList.getItems().setAll("❌ Failed to load rooms"));
                    }
                }, Runnable::run);
            } catch (Exception e) {
                Platform.runLater(() -> roomsList.getItems().setAll("❌ Failed to load rooms"));
                e.printStackTrace();
            }
        }));
        roomRefresher.setCycleCount(Animation.INDEFINITE);
        roomRefresher.play();
    }

    private Button styledButton(String text) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: #4d2c1d; -fx-text-fill: white; -fx-font-size: 16px; -fx-background-radius: 8;");
        button.setPrefWidth(200);
        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: #704a30; -fx-text-fill: white; -fx-font-size: 16px; -fx-background-radius: 8;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-background-color: #4d2c1d; -fx-text-fill: white; -fx-font-size: 16px; -fx-background-radius: 8;"));
        return button;
    }

    private void addGlow(Label label) {
        DropShadow glow = new DropShadow();
        glow.setColor(Color.GOLD);
        glow.setRadius(15);
        glow.setSpread(0.3);
        label.setEffect(glow);
    }

    private void playMusic() {
        try {
            Media media = new Media(new File("Utils/drums_of_liberations.mp3").toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            mediaPlayer.setVolume(0.4);
            mediaPlayer.play();
        } catch (Exception e) {
            System.out.println("⚠️ Failed to load background music");
        }
    }
}
