package UI;

import Client.AuthServiceClient;
import catan.Catan;
import com.google.common.util.concurrent.ListenableFuture;
import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
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

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class HomePage extends Application {

    private final AuthServiceClient authClient;
    private VBox roomsContainer;
    private MediaPlayer backgroundMusicPlayer;
    private Timeline roomRefresher;
    private AudioClip clickSound;
    private final String userId;

    public HomePage(String userId, AuthServiceClient authClient) {
        this.userId = userId;
        this.authClient = authClient;
    }

    @Override
    public void start(Stage primaryStage) {
        StackPane root = new StackPane();

        clickSound = new AudioClip(
                Objects.requireNonNull(getClass().getResource("/Utils/mouse_click.mp3")).toExternalForm()
        );
        clickSound.setVolume(0.5);

        // ✅ רקע מים מונפש (GIF)
        ImageView background = new ImageView(new Image(
                Objects.requireNonNull(getClass().getResource("/Utils/water_background.gif")).toExternalForm(),
                true // ✅ load in background = לא נועל את הקובץ
        ));
        background.setFitWidth(800);
        background.setFitHeight(600);
        background.setPreserveRatio(false);

        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.maxWidthProperty().bind(root.widthProperty().multiply(0.8));

        Label userLabel = new Label("Welcome to Catan!");
        userLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 28));
        userLabel.setTextFill(Color.WHITE);
        addGlow(userLabel);

        TranslateTransition slideIn = new TranslateTransition(Duration.seconds(1), userLabel);
        slideIn.setFromY(-50);
        slideIn.setToY(0);
        slideIn.setInterpolator(Interpolator.EASE_OUT);
        slideIn.play();

        Button createRoomBtn = styledButton("➕ Create Room");
        createRoomBtn.setOnAction(e -> {
            clickSound.play();
            showCreateRoomDialog();
        });

        Button invitesBtn = styledButton("📨 Invitations");
        invitesBtn.setOnAction(e -> {
            clickSound.play();
            showInvitations();
        });

        Label roomsLabel = new Label("Available Rooms:");
        roomsLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 18));
        roomsLabel.setTextFill(Color.WHITE);
        addGlow(roomsLabel);

        roomsContainer = new VBox(10);
        roomsContainer.setPrefSize(300, 250);
        roomsContainer.setPadding(new Insets(10));
        roomsContainer.setAlignment(Pos.TOP_CENTER);
        roomsContainer.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1); -fx-background-radius: 10;");
        roomsContainer.maxWidthProperty().bind(content.maxWidthProperty().multiply(0.75));

        ScrollPane scroll = new ScrollPane(roomsContainer);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(250);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        Button muteBtn = styledButton("🔇 Mute");
        clickSound.play();
        muteBtn.setOnAction(e -> {
            if (backgroundMusicPlayer != null) {
                backgroundMusicPlayer.setMute(!backgroundMusicPlayer.isMute());
                muteBtn.setText(backgroundMusicPlayer.isMute() ? "🔊 Unmute" : "🔇 Mute");
            }
        });

        content.getChildren().addAll(userLabel, createRoomBtn, invitesBtn, roomsLabel, scroll, muteBtn);
        root.getChildren().addAll(background, content);

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("Catan - Home");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);  // גודל קבוע
        primaryStage.show();

        playMusic();
        refreshRoomListPeriodically();
    }

    private void showCreateRoomDialog() {
        Stage dialog = new Stage();
        StackPane root = new StackPane();

        // ✅ רקע - מפה עתיקה
        ImageView background = new ImageView(new Image(
                Objects.requireNonNull(getClass().getResource("/Utils/map_old_style.png")).toExternalForm()));
        background.setFitWidth(400);
        background.setFitHeight(250);
        background.setPreserveRatio(false);

        VBox content = new VBox(15);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(30));
        content.setMaxWidth(350);
        content.setStyle("-fx-background-color: rgba(255, 248, 220, 0.8); -fx-background-radius: 15;");

        Label title = new Label("Create New Group");
        title.setFont(Font.font("Papyrus", FontWeight.BOLD, 24));
        title.setTextFill(Color.DARKRED);
        title.setEffect(new DropShadow(5, Color.GOLD));

        TextField nameField = new TextField();
        nameField.setPromptText("e.g., Pirate Legends");
        nameField.setStyle("-fx-font-size: 14px;");

        Label statusLabel = new Label();
        statusLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 12));
        statusLabel.setTextFill(Color.DARKBLUE);

        Button createBtn = new Button("✅ Create");
        createBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 8;");
        createBtn.setOnAction(e -> {
            String groupName = nameField.getText().trim();
            if (groupName.isEmpty()) {
                showAlert("Error", "Group name cannot be empty", Alert.AlertType.WARNING);
                return;
            }

            statusLabel.setText("⏳ Creating group...");

            ListenableFuture<Catan.GroupResponse> createFuture = authClient.createGroup(userId, groupName);
            createFuture.addListener(() -> {
                try {
                    Catan.GroupResponse response = createFuture.get();
                    Platform.runLater(() -> {
                        if (response.getSuccess()) {
                            showAlert("Success", response.getMessage(), Alert.AlertType.INFORMATION);
                            statusLabel.setText("✅ Created!");
                            dialog.close();
                            refreshRoomListPeriodically(); // רענון מיידי
                        } else {
                            statusLabel.setText("❌ " + response.getMessage());
                        }
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Platform.runLater(() -> {
                        statusLabel.setText("❌ Failed: " + ex.getMessage());
                    });
                }
            }, Runnable::run);
        });

        content.getChildren().addAll(title, nameField, createBtn, statusLabel);
        root.getChildren().addAll(background, content);

        Scene scene = new Scene(root, 400, 250);
        dialog.setScene(scene);
        dialog.setTitle("Create Group");

        // ✅ אנימציית הופעה
        root.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        dialog.show();
    }

    private void refreshRoomListPeriodically() {
        if (roomRefresher != null) roomRefresher.stop();  // מונעים כפילויות של Timeline

        roomRefresher = new Timeline(new KeyFrame(Duration.seconds(5), ev -> {
            try {
                ListenableFuture<Catan.AllGroupsResponse> future = authClient.getAllGroups(userId);
                future.addListener(() -> {
                    try {
                        Catan.AllGroupsResponse response = future.get();
                        List<Catan.GroupInfo> uniqueGroups = response.getGroupsList().stream()
                                .collect(Collectors.toMap(
                                        Catan.GroupInfo::getGroupId,
                                        g -> g,
                                        (g1, g2) -> g1))
                                .values()
                                .stream()
                                .toList();

                        Platform.runLater(() -> {
                            roomsContainer.getChildren().clear();
                            if (uniqueGroups.isEmpty()) {
                                Label noRooms = new Label("No rooms available");
                                noRooms.setTextFill(Color.WHITE);
                                noRooms.setFont(Font.font("Verdana", FontPosture.ITALIC, 14));
                                roomsContainer.getChildren().add(noRooms);
                            } else {
                                for (Catan.GroupInfo group : uniqueGroups) {
                                    HBox card = createGroupCard(group);
                                    card.prefWidthProperty().bind(roomsContainer.widthProperty().subtract(20));

                                    roomsContainer.getChildren().add(card);
                                }
                            }
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> {
                            roomsContainer.getChildren().clear();
                            Label error = new Label("❌ Failed to load rooms");
                            error.setTextFill(Color.RED);
                            roomsContainer.getChildren().add(error);
                        });
                    }
                }, Runnable::run);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
        roomRefresher.setCycleCount(Animation.INDEFINITE);
        roomRefresher.play();
    }

    private void showInvitations() {
        Stage inviteStage = new Stage();
        StackPane root = new StackPane();

        ImageView background = new ImageView(new Image(
                Objects.requireNonNull(getClass().getResource("/Utils/map_old_style.png")).toExternalForm()));
        background.setFitWidth(600);
        background.setFitHeight(500);
        background.setPreserveRatio(false);

        VBox invitationList = new VBox(15);
        invitationList.setAlignment(Pos.CENTER);
        invitationList.setPadding(new Insets(40));
        invitationList.setMaxWidth(550);
        invitationList.setStyle("-fx-background-color: rgba(255, 248, 220, 0.8); -fx-background-radius: 20;");

        Label title = new Label("Your Invitations");
        title.setFont(Font.font("Papyrus", FontWeight.BOLD, 30));
        title.setTextFill(Color.DARKRED);
        title.setEffect(new DropShadow(5, Color.GOLD));

        invitationList.getChildren().add(title);

        authClient.getUserInvites(userId).addListener(() -> {
            try {
                Catan.InviteListResponse response = authClient.getUserInvites(userId).get();
                List<InviteInfo> inviteInfos = response.getInvitesList().stream()
                        .map(proto -> new InviteInfo(
                                proto.getInviteId(),
                                proto.getGroupId(),
                                proto.getSenderId(),
                                proto.getInvitedUserId(),
                                "Group: " + proto.getGroupId(),     // או עם getGroupName אם יש
                                "From: " + proto.getSenderId()      // או עם getSenderUsername אם יש
                        ))
                        .toList();

                Platform.runLater(() -> {
                    for (InviteInfo invite : inviteInfos) {
                        invitationList.getChildren().add(createInviteCard(invite, invitationList));
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    Label error = new Label("⚠ Failed to load invites");
                    error.setTextFill(Color.DARKRED);
                    error.setFont(Font.font("Verdana", FontWeight.BOLD, 16));
                    invitationList.getChildren().add(error);
                });
            }
        }, Runnable::run);

        root.getChildren().addAll(background, invitationList);

        Scene scene = new Scene(root, 600, 500);
        inviteStage.setScene(scene);
        inviteStage.setTitle("Invitations");
        inviteStage.show();

        ScaleTransition scale = new ScaleTransition(Duration.millis(400), invitationList);
        scale.setFromX(0.9);
        scale.setFromY(0.9);
        scale.setToX(1);
        scale.setToY(1);

        FadeTransition fade = new FadeTransition(Duration.millis(400), invitationList);
        fade.setFromValue(0);
        fade.setToValue(1);

        new ParallelTransition(scale, fade).play();
    }

    private HBox createGroupCard(Catan.GroupInfo group) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: #f5e6cc; -fx-border-color: #8b5a2b; -fx-border-width: 3; -fx-background-radius: 10;");
        card.setCursor(Cursor.HAND);
        card.setEffect(new DropShadow(5, Color.BURLYWOOD));
        card.setMinHeight(60);

        // אייקון
        ImageView icon = new ImageView(new Image(Objects.requireNonNull(getClass().getResource("/Utils/compass_icon.png")).toExternalForm()));
        icon.setFitWidth(32);
        icon.setFitHeight(32);

        // טקסט
        Label nameLabel = new Label(group.getGroupName());
        nameLabel.setFont(Font.font("Papyrus", FontWeight.BOLD, 18));
        nameLabel.setTextFill(Color.DARKBLUE);

        Label creatorLabel = new Label("Created by " + group.getCreatorUsername());
        creatorLabel.setFont(Font.font("Papyrus", 14));
        creatorLabel.setTextFill(Color.SADDLEBROWN);

        VBox testBox = new VBox(5, nameLabel, creatorLabel);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        card.getChildren().addAll(icon, testBox, spacer);

        card.setOnMouseClicked(e -> {
            clickSound.play();

            Platform.runLater(() -> {
                try {
                    GroupScreen groupScreen = new GroupScreen(group, authClient, userId);
                    Stage groupStage = new Stage();
                    groupScreen.start(groupStage);
                    Stage homeStage = (Stage) card.getScene().getWindow();
                    homeStage.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showAlert("Error", "Failed to open group screen: " + ex.getMessage(), Alert.AlertType.ERROR);
                }
            });
        });

        card.setOnMouseEntered(e -> {
            ScaleTransition scaleUp = new ScaleTransition(Duration.millis(150), card);
            scaleUp.setToX(1.03);
            scaleUp.setToY(1.03);
            scaleUp.play();
            card.setEffect(new DropShadow(10, Color.DARKGREEN));
        });

        card.setOnMouseExited(e -> {
            ScaleTransition scaleDown = new ScaleTransition(Duration.millis(150), card);
            scaleDown.setToX(1);
            scaleDown.setToY(1);
            scaleDown.play();
            card.setEffect(null);
        });

        return card;
    }

    private HBox createInviteCard(InviteInfo invite, VBox invitationList) {
        HBox card = new HBox(20);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #e5d1b8; -fx-border-color: #8b5a2b; -fx-border-width: 2; -fx-background-radius: 10;");

        // === אפקט ריחוף לכרטיס ===
        card.setOnMouseEntered(e -> {
            ScaleTransition scaleUp = new ScaleTransition(Duration.millis(150), card);
            scaleUp.setToX(1.02);
            scaleUp.setToY(1.02);
            scaleUp.play();
            card.setEffect(new DropShadow(8, Color.SADDLEBROWN));
        });

        card.setOnMouseExited(e -> {
            ScaleTransition scaleDown = new ScaleTransition(Duration.millis(150), card);
            scaleDown.setToX(1);
            scaleDown.setToY(1);
            scaleDown.play();
            card.setEffect(null);
        });

        Label groupLabel = new Label(invite.groupName);
        groupLabel.setFont(Font.font("Papyrus", FontWeight.BOLD, 20));
        groupLabel.setTextFill(Color.SADDLEBROWN);

        Label fromLabel = new Label("invited by " + invite.fromUsername);
        fromLabel.setFont(Font.font("Papyrus", 16));
        fromLabel.setTextFill(Color.DARKGOLDENROD);

        Button accept = new Button("✔ Accept");
        Button decline = new Button("❌ Decline");

        accept.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 5;");
        DropShadow pulseGlow = new DropShadow(10, Color.GOLD);
        accept.setEffect(pulseGlow);
        Timeline pulse = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(pulseGlow.radiusProperty(), 10)),
                new KeyFrame(Duration.millis(500), new KeyValue(pulseGlow.radiusProperty(), 20)),
                new KeyFrame(Duration.millis(1000), new KeyValue(pulseGlow.radiusProperty(), 10))
        );
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.play();

        decline.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-background-radius: 5;");

        // Hover style
        accept.setOnMouseEntered(e -> accept.setStyle("-fx-background-color: #66bb6a; -fx-text-fill: white; -fx-background-radius: 5;"));
        accept.setOnMouseExited(e -> accept.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 5;"));

        decline.setOnMouseEntered(e -> decline.setStyle("-fx-background-color: #e57373; -fx-text-fill: white; -fx-background-radius: 5;"));
        decline.setOnMouseExited(e -> decline.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-background-radius: 5;"));

        // === Accept ===
        accept.setOnAction(e -> {
            clickSound.play();
            authClient.respondToInvite(
                    invite.inviteId(),
                    invite.groupId(),
                    invite.senderId(),
                    invite.receiverId(),
                    Catan.InviteResponseStatus.ACCEPTED
            ).addListener(() -> {
                try {
                    Catan.GroupResponse response = authClient
                            .respondToInvite(invite.inviteId(), invite.groupId(), invite.senderId(), invite.receiverId(), Catan.InviteResponseStatus.ACCEPTED)
                            .get();

                    Platform.runLater(() -> {
                        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), card);
                        fadeOut.setFromValue(1.0);
                        fadeOut.setToValue(0.0);
                        fadeOut.setOnFinished(evt -> {
                            invitationList.getChildren().remove(card);
                            showAlert("Invitation Accepted", response.getMessage(), Alert.AlertType.INFORMATION);
                        });
                        fadeOut.play();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> showShakeError(card, "Failed to respond: " + ex.getMessage()));
                }
            }, Runnable::run);
        });


        // === Decline ===
        decline.setOnAction(e -> {
            clickSound.play();
            authClient.respondToInvite(
                    invite.inviteId(),
                    invite.groupId(),
                    invite.senderId(),
                    invite.receiverId(),
                    Catan.InviteResponseStatus.DECLINED
            ).addListener(() -> {
                try {
                    Catan.GroupResponse response = authClient
                            .respondToInvite(invite.inviteId(), invite.groupId(), invite.senderId(), invite.receiverId(), Catan.InviteResponseStatus.DECLINED)
                            .get();

                    Platform.runLater(() -> {
                        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), card);
                        fadeOut.setFromValue(1.0);
                        fadeOut.setToValue(0.0);
                        fadeOut.setOnFinished(evt -> {
                            invitationList.getChildren().remove(card);
                            showAlert("Invitation Declined", response.getMessage(), Alert.AlertType.INFORMATION);
                        });
                        fadeOut.play();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> showShakeError(card, "Failed to respond: " + ex.getMessage()));
                }
            }, Runnable::run);
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox textBox = new VBox(groupLabel, fromLabel);
        card.getChildren().addAll(textBox, spacer, accept, decline);
        return card;
    }

    private Button styledButton(String text) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: #4d2c1d; -fx-text-fill: white; -fx-font-size: 16px; -fx-background-radius: 8;");
        button.setPrefWidth(200);
        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: #704a30; -fx-text-fill: white; -fx-font-size: 16px; -fx-background-radius: 8;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-background-color: #4d2c1d; -fx-text-fill: white; -fx-font-size: 16px; -fx-background-radius: 8;"));
        return button;
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // === פונקציית רעידה על שגיאה ===
    private void showShakeError(Node node, String message) {
        TranslateTransition shake = new TranslateTransition(Duration.millis(60), node);
        shake.setFromX(0);
        shake.setByX(10);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.setOnFinished(ev -> showAlert("Error", message, Alert.AlertType.ERROR));
        shake.play();
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
            var resource = getClass().getResource("/Utils/drums_of_liberation.mp3");
            if (resource == null) {
                System.out.println("⚠️ Resource not found: /Utils/drums_of_liberation.mp3");
                return;
            }

            Media media = new Media(resource.toExternalForm());
            backgroundMusicPlayer = new MediaPlayer(media);
            backgroundMusicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            backgroundMusicPlayer.setVolume(0.4);
            backgroundMusicPlayer.play();
        } catch (Exception e) {
            System.out.println("⚠️ Failed to load or play background music");
            e.printStackTrace();
        }
    }

    private record InviteInfo(String inviteId,
                              String groupId,
                              String senderId,
                              String receiverId,
                              String groupName,
                              String fromUsername
    ) {}
}
