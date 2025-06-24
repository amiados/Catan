package UI;

import Client.AuthServiceClient;
import catan.Catan;
import io.grpc.stub.StreamObserver;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class GroupScreen extends Application {
    private final Catan.GroupInfo groupInfo;
    private final AuthServiceClient authClient;
    private final String userId;
    private final String groupId;
    private final String groupName;
    private VBox gamesContainer;
    private VBox chatContainer;

    public GroupScreen(Catan.GroupInfo groupInfo, AuthServiceClient authClient, String userId) {
        this.groupInfo = groupInfo;
        this.authClient = authClient;
        this.userId = userId;
        this.groupId = groupInfo.getGroupId();
        this.groupName = groupInfo.getGroupName();
    }

    @Override
    public void start(Stage stage){
        StackPane root = new StackPane();

        ImageView background = new ImageView(new Image(
                Objects.requireNonNull(getClass().getResource("/Utils/map_old_style.png")).toExternalForm(),
                true
        ));

        background.setFitWidth(800);
        background.setFitHeight(600);
        background.setPreserveRatio(false);

        VBox content = new VBox(20);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(20));
        content.setMaxWidth(760);
        content.setStyle("-fx-background-color: rgba(255,248,220,0.85); -fx-background-radius: 12;");

        // 📛 כותרת
        Label title = new Label("Group: " + groupName);
        title.setFont(Font.font("Papyrus", FontWeight.BOLD, 28));
        title.setTextFill(Color.DARKRED);
        title.setEffect(new DropShadow(5, Color.GOLD));

        Label gamesLabel = new Label("Games:");
        gamesLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 18));

        Button newGameBtn = styledButton("➕ New Game");
        newGameBtn.setOnAction(e -> {
            // TODO: יצירת משחק חדש ב־backend
            fetchAndRenderGames();
        });

        gamesContainer = new VBox(10);
        gamesContainer.setPrefHeight(120);
        gamesContainer.setPadding(new Insets(5));
        gamesContainer.setStyle("-fx-background-color: rgba(255,255,255,0.6); -fx-background-radius: 8;");

        HBox gamesHeader = new HBox(10, gamesLabel, newGameBtn);
        gamesHeader.setAlignment(Pos.CENTER_LEFT);

        // 📜 היסטוריית הודעות
        Label chatLabel = new Label("Chat:");
        chatLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 18));
        chatLabel.setTextFill(Color.SADDLEBROWN);

        chatContainer = new VBox(5);
        chatContainer.setPrefHeight(200);
        chatContainer.setPadding(new Insets(5));
        chatContainer.setStyle("-fx-background-color: rgba(255,255,255,0.6); -fx-background-radius: 8;");

        TextField messageField = new TextField();
        messageField.setPromptText("Type a message...");
        messageField.setOnKeyPressed(evt -> {
            if (evt.getCode() == KeyCode.ENTER) {
                sendMessage(messageField.getText());
                messageField.clear();
            }
        });

        Button sendBtn = styledButton("📨 Send");
        sendBtn.setOnAction(e -> {
            sendMessage(messageField.getText());
            messageField.clear();
        });

        HBox chatInput = new HBox(10, messageField, sendBtn);
        chatInput.setAlignment(Pos.CENTER);

        content.getChildren().addAll(
                title,
                gamesHeader,
                gamesContainer,
                chatLabel,
                chatContainer,
                chatInput
        );

        root.getChildren().addAll(background, content);
        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.setTitle("Catan Group");
        stage.show();

        // Entrance animation
        FadeTransition ft = new FadeTransition(Duration.millis(400), content);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();

        // initial load
        fetchAndRenderGames();
        fetchAndRenderChat();
    }

    private void fetchAndRenderGames() {
        authClient.listGroupGames(groupId)
                .addListener(() -> {
                    try {
                        Catan.GameListResponse response = authClient.listGroupGames(groupId).get();
                        List<String> gameIds = response.getGameIdsList(); // תוסיף מתודה שמחזירה את הפלט האחרון

                        Platform.runLater(() -> {
                            gamesContainer.getChildren().clear();
                            for (String gid : gameIds) {
                                Button btn = new Button("▶️ Game " + gid.substring(0, 6));
                                btn.setOnAction(e -> {
                                    // TODO: open GameScreen for gid
                                });
                                btn.setMaxWidth(Double.MAX_VALUE);
                                gamesContainer.getChildren().add(btn);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
        }, Runnable::run);
    }

    private void fetchAndRenderChat() {
        authClient.subscribeGroupMessages(groupId, userId, new StreamObserver<Catan.GroupChatMessage>() {
            @Override
            public void onNext(Catan.GroupChatMessage groupChatMessage) {
                Platform.runLater(() -> {
                    String sender = groupChatMessage.getFromUserId();
                    String text = new String(groupChatMessage.getContent().toByteArray());
                    Label label = new Label(sender + ": " + text);
                    chatContainer.getChildren().add(label);
                });
            }

            @Override
            public void onError(Throwable throwable) {
                throwable.printStackTrace(); // תיעוד תקלה
            }

            @Override
            public void onCompleted() {
                Platform.runLater(() -> {
                    Label done = new Label("📴 Disconnected from chat.");
                    chatContainer.getChildren().add(done);
                });
            }
        });
    }

    private void sendMessage(String text) {
        if (text.isBlank()) return;

        authClient.sendGroupMessage(groupId, userId, text.getBytes())
                .addListener(() -> {
                    Platform.runLater(() -> {
                        Label me = new Label("You: " + text);
                        me.setStyle("-fx-text-fill: green;");
                        chatContainer.getChildren().add(me);
                    });
                }, Runnable::run);
    }

    private Button styledButton(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: #4d2c1d; -fx-text-fill: white; -fx-background-radius: 6;");
        b.setFont(Font.font("Verdana", FontWeight.BOLD, 14));
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: #704a30; -fx-text-fill: white; -fx-background-radius: 6;"));
        b.setOnMouseExited(e -> b.setStyle("-fx-background-color: #4d2c1d; -fx-text-fill: white; -fx-background-radius: 6;"));
        return b;
    }
}
