package UI;

import Client.AuthServiceClient;
import Utils.ColorMapper;
import catan.Board;
import catan.Catan;
import catan.PieceColor;
import com.google.common.util.concurrent.ListenableFuture;
import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameLobbyWindow extends Application {

    private final String[] availableColors = {"Red", "Blue", "White", "Orange", "Black", "Green"};
    private static final Map<String, Button> colorButtons = new HashMap<>();
    private static final Set<String> takenColors = ConcurrentHashMap.newKeySet();
    private static final ListView<String> playerListView  = new ListView<>();
    private final Map<String, String> userIdToPlayerId = new HashMap<>();
    private String selectedColor = null;

    private final Label statusLabel = new Label("Select your color:");
    private final Button startButton = new Button("Start Game");
    private final Button leaveButton = new Button("Leave Lobby");

    private final String userId;
    private final String groupId;
    private final AuthServiceClient authClient;
    private final String gameId;
    private ScheduledExecutorService scheduler;

    public GameLobbyWindow(String userId, String groupId, AuthServiceClient authClient, String gameId) {
        this.userId = userId;
        this.groupId = groupId;
        this.authClient = authClient;
        this.gameId = gameId;
    }

    @Override
    public void start(Stage primaryStage) {
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);

        Label title = new Label("Game Lobby");
        title.setFont(Font.font("Arial", 28));
        title.setTextFill(Color.DARKBLUE);
        title.setEffect(new DropShadow());

        HBox colorsBox = new HBox(10);
        colorsBox.setAlignment(Pos.CENTER);

        for (String color : availableColors) {
            Button colorBtn = new Button(color);
            colorBtn.setPrefWidth(80);
            colorBtn.setStyle("-fx-background-color: " + color.toLowerCase() + "; -fx-text-fill: #ae0fd3; -fx-font-weight: bold;");
            colorBtn.setFont(Font.font(16));
            colorBtn.setOnAction(e -> handleColorSelection(color));
            colorButtons.put(color, colorBtn);
            colorsBox.getChildren().add(colorBtn);

            TranslateTransition slide = new TranslateTransition(Duration.millis(600), colorBtn);
            slide.setFromY(-50);
            slide.setToY(0);
            slide.setInterpolator(Interpolator.EASE_OUT);
            slide.play();
        }

        VBox playersBox = new VBox(5, new Label("Players:"), playerListView);
        playersBox.setAlignment(Pos.CENTER);
        playerListView.setPrefHeight(100);
        playerListView.setMaxWidth(250);

        startButton.setFont(Font.font(16));
        startButton.setDisable(true);
        startButton.setOnAction(e -> startGameRequest(primaryStage));

        leaveButton.setFont(Font.font(14));
        leaveButton.setOnAction(e -> {
            ListenableFuture<Catan.GameResponse> leaveFuture =
                    authClient.leaveGame(groupId, gameId, userId);
            leaveFuture.addListener(() -> {
                try {
                    Catan.GameResponse response = leaveFuture.get();
                    if (response.getSuccess()) {
                        // אם הצליח לעזוב, נביא מידע על הקבוצה
                        ListenableFuture<Catan.GroupInfo> infoFuture =
                                authClient.getGroupInfo(groupId);
                        infoFuture.addListener(() -> {
                            try {
                                Catan.GroupInfo info = infoFuture.get();
                                Platform.runLater(() -> {
                                    showToast("You left the game.");

                                    // פתיחת GroupScreen חדש
                                    GroupScreen groupScreen = new GroupScreen(info, authClient, userId);
                                    try {
                                        groupScreen.start(new Stage());
                                    } catch (Exception ex) {
                                        showAlert("Failed to open group screen: " + ex.getMessage());
                                    }

                                    // סגירת חלון לובי
                                    ((Stage) leaveButton.getScene().getWindow()).close();
                                });
                            } catch (Exception ex) {
                                Platform.runLater(() -> showAlert("Failed to load group info: " + ex.getMessage()));
                            }
                        }, Runnable::run);
                    } else {
                        Platform.runLater(() -> showAlert("Failed to leave game: " + response.getMessage()));
                    }
                } catch (Exception ex) {
                    Platform.runLater(() -> showAlert("Error leaving game: " + ex.getMessage()));
                }
            }, Runnable::run);
        });

        // Pulse effect
        ScaleTransition pulse = new ScaleTransition(Duration.seconds(1), startButton);
        pulse.setFromX(1);
        pulse.setFromY(1);
        pulse.setToX(1.1);
        pulse.setToY(1.1);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.play();

        Stop[] stops = new Stop[]{new Stop(0, Color.LIGHTBLUE), new Stop(1, Color.LIGHTGREEN)};
        LinearGradient gradient = new LinearGradient(0, 0, 1, 1, true, null, Arrays.asList(stops));
        BackgroundFill fill = new BackgroundFill(gradient, CornerRadii.EMPTY, Insets.EMPTY);
        Background bg = new Background(fill);
        root.setBackground(bg);

        Timeline gradientTimeline = new Timeline(
                new KeyFrame(Duration.seconds(0), new KeyValue(root.opacityProperty(), 1.0)),
                new KeyFrame(Duration.seconds(10), new KeyValue(root.opacityProperty(), 1.0))
        );
        gradientTimeline.setCycleCount(Animation.INDEFINITE);
        gradientTimeline.play();

        root.getChildren().addAll(
                statusLabel,
                title,
                colorsBox,
                playersBox,
                startButton,
                leaveButton
        );

        StackPane rootContent = new StackPane(root);
        Scene scene = new Scene(rootContent, 400, 400);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Game Lobby");
        primaryStage.show();

        fetchLobbyState();
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::fetchLobbyState, 0, 3, TimeUnit.SECONDS);

        authClient.subscribeToGameEvents(gameId, userId, event -> {
            if (event.getType().equals("START")) {
                Platform.runLater(() -> {
                    showToast("Game started!");
                    launchGameBoard(primaryStage);
                });
            }
        });
    }

    private void fetchLobbyState() {

        ListenableFuture<Catan.GamePlayersResponse> future =
                authClient.getGamePlayers(groupId, gameId, userId);
        future.addListener(() -> {
            try {
                Catan.GamePlayersResponse response = future.get();

                List<String> players =  new ArrayList<>();
                takenColors.clear();

                for (Catan.PlayerProto player : response.getPlayersList()) {

                    players.add(player.getUsername() + (player.getIsTurn() ? " (Your Turn)" : ""));
                    userIdToPlayerId.put(player.getUserId(), player.getPlayerId());

                    if (!player.getColor().isEmpty()) {
                        Catan.Color protoColor = Catan.Color.valueOf(player.getColor());
                        PieceColor pieceColor = ColorMapper.fromProtoColor(protoColor);
                        takenColors.add(prettifyColorName(pieceColor.name()));                    }
                }

                Platform.runLater(() -> {
                    updateColorButtons();
                    updatePlayerList(players);
                });

            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Failed to fetch lobby state: " + e.getMessage()));
            }
        }, Runnable::run);
    }

    private void updateColorButtons() {
        for (String color : availableColors) {
            Button btn = colorButtons.get(color);
            if (!color.equals(selectedColor)) {
                btn.setDisable(takenColors.contains(color));
            }
        }
    }

    private void handleColorSelection(String color) {
        if (takenColors.contains(color)) return;

        if (selectedColor != null) {
            takenColors.remove(selectedColor);
            colorButtons.get(selectedColor).setDisable(false);
        }

        selectedColor = color;
        takenColors.add(color);
        colorButtons.get(color).setDisable(true);
        statusLabel.setText("You selected: " + color);
        startButton.setDisable(false);

        highlightSelectedColor(color); // שינוי ויזואלי

        // Bounce animation
        ScaleTransition bounce = new ScaleTransition(Duration.millis(300), colorButtons.get(color));
        bounce.setFromX(1);
        bounce.setFromY(1);
        bounce.setToX(1.3);
        bounce.setToY(1.3);
        bounce.setAutoReverse(true);
        bounce.setCycleCount(2);
        bounce.play();

        showToast("Color " + color + " selected");

        try {
            PieceColor pieceColor = PieceColor.valueOf(color.toUpperCase());
            Catan.Color protoColor = ColorMapper.toProtoColor(pieceColor);

            String playerId = userIdToPlayerId.get(userId);

            if (playerId == null) {
                showAlert("Player ID not found for current user.");
                return;
            }

            ListenableFuture<Catan.GameResponse> future = authClient.updateColor(playerId, gameId, protoColor);
            future.addListener(() -> {
                try {
                    Catan.GameResponse response = future.get();
                    if (!response.getSuccess()) {
                        Platform.runLater(() -> showAlert("Server rejected color: " + response.getMessage()));
                    }
                } catch (Exception e) {
                    Platform.runLater(() -> showAlert("Error updating color: " + e.getMessage()));
                }
            }, Runnable::run);
        } catch (IllegalArgumentException ex) {
            showAlert("Invalid color selection.");
        }
    }

    private void showToast(String message) {
        Label toast = new Label(message);
        toast.setStyle("-fx-background-color: black; -fx-text-fill: white; -fx-padding: 10;");
        toast.setOpacity(0);

        StackPane root = (StackPane) playerListView.getScene().getRoot();
        root.getChildren().add(toast);
        StackPane.setAlignment(toast, Pos.TOP_CENTER);

        FadeTransition show = new FadeTransition(Duration.seconds(0.5), toast);
        show.setFromValue(0);
        show.setToValue(1);

        PauseTransition pause = new PauseTransition(Duration.seconds(2));

        FadeTransition hide = new FadeTransition(Duration.seconds(0.5), toast);
        hide.setFromValue(1);
        hide.setToValue(0);
        hide.setOnFinished(e -> root.getChildren().remove(toast));

        SequentialTransition seq = new SequentialTransition(show, pause, hide);
        seq.play();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void updatePlayerList(List<String> players) {
        Platform.runLater(() -> playerListView.getItems().setAll(players));
    }

    private void startGameRequest(Stage stage) {
        VBox countdownBox = new VBox(15);
        countdownBox.setAlignment(Pos.CENTER);

        Label countdownLabel = new Label("Starting in 5...");
        countdownLabel.setFont(Font.font(28));

        Button cancelButton = new Button("ביטול");
        cancelButton.setFont(Font.font(16));

        countdownBox.getChildren().addAll(countdownLabel, cancelButton);

        Scene countdownScene = new Scene(countdownBox, 400, 300);
        stage.setScene(countdownScene);

        IntegerProperty secondsLeft = new SimpleIntegerProperty(5);

        // עטיפת Timeline כדי לאפשר שימוש מתוך lambda
        final Timeline[] timelineRef = new Timeline[1];

        cancelButton.setOnAction(e -> {
            if (timelineRef[0] != null) {
                timelineRef[0].stop();
            }
            fetchLobbyState(); // חזרה ללובי ועדכון מצב
            showToast("Game start cancelled");
            start(stage);
        });

        timelineRef[0] = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> {
                    int time = secondsLeft.get() - 1;
                    secondsLeft.set(time);
                    countdownLabel.setText("Starting in " + time + "...");
                    if (time <= 0) {
                        timelineRef[0].stop();
                        countdownLabel.setText("Game starts!");

                        // קריאה לשרת להתחלת המשחק
                        ListenableFuture<Catan.GameResponse> future =
                                authClient.startGame(groupId, gameId, userId);

                        future.addListener(() -> {
                            try {
                                Catan.GameResponse response = future.get();
                                if (response.getSuccess()) {
                                    Platform.runLater(() -> launchGameBoard(stage));
                                } else {
                                    Platform.runLater(() -> showAlert("Failed to start game: " + response.getMessage()));
                                }
                            } catch (Exception e) {
                                Platform.runLater(() -> showAlert("Error starting game: " + e.getMessage()));
                            }
                        }, Runnable::run);
                    }
                })
        );
        timelineRef[0].setCycleCount(5);
        timelineRef[0].play();
    }

    private void launchGameBoard(Stage stage) {
        try {
            Board board = new Board(userId, groupId, gameId, authClient);
            board.start(new Stage());
            stage.close();
        } catch (Exception ex) {
            showAlert("Failed to launch game board: " + ex.getMessage());
        }
    }

    private String prettifyColorName(String colorEnum) {
        return colorEnum.charAt(0) + colorEnum.substring(1).toLowerCase();
    }

    private void highlightSelectedColor(String selected) {
        for (String color : availableColors) {
            Button btn = colorButtons.get(color);
            if (color.equals(selected)) {
                btn.setStyle("-fx-background-color: " + color.toLowerCase() + "; -fx-text-fill: white; -fx-border-color: gold; -fx-border-width: 3;");
            } else {
                btn.setStyle("-fx-background-color: " + color.toLowerCase() + "; -fx-text-fill: #ae0fd3; -fx-font-weight: bold;");
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
