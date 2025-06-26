package UI;

import Client.AuthServiceClient;
import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
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

public class GameLobbyWindow extends Application {

    private final String[] availableColors = {"Red", "Blue", "White", "Orange", "Black", "Green"};
    private static final Map<String, Button> colorButtons = new HashMap<>();
    private static final Set<String> takenColors = ConcurrentHashMap.newKeySet();
    private static final ListView<String> playerListView  = new ListView<>();
    private String selectedColor = null;

    private final Label statusLabel = new Label("Select your color:");
    private final Button startButton = new Button("Start Game");

    private final String userId;
    private final String groupId;
    private final AuthServiceClient authClient;

    public GameLobbyWindow(String userId, String groupId, AuthServiceClient authClient) {
        this.userId = userId;
        this.groupId = groupId;
        this.authClient = authClient;
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
            colorBtn.setStyle("-fx-background-color: " + color.toLowerCase() + "; -fx-text-fill: white; -fx-font-weight: bold;");
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
        startButton.setOnAction(e -> startCountdown(primaryStage));


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
                startButton
        );

        Scene scene = new Scene(root, 400, 400);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Game Lobby");
        primaryStage.show();

        updatePlayerList(List.of("You"));
    }

    private void handleColorSelection(String color) {
        if (takenColors.contains(color)) return;

        if (selectedColor != null) {
            colorButtons.get(selectedColor).setDisable(false);
            takenColors.remove(selectedColor);
        }

        selectedColor = color;
        takenColors.add(color);
        colorButtons.get(color).setDisable(true);

        statusLabel.setText("You selected: " + color);
        startButton.setDisable(false);

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


    private void updatePlayerList(List<String> players) {
        Platform.runLater(() -> {
            playerListView.getItems().setAll(players);
        });
    }


    private void startCountdown(Stage stage) {
        VBox countdownBox = new VBox(10);
        countdownBox.setAlignment(Pos.CENTER);
        Label countdownLabel = new Label("Starting in 5...");
        countdownLabel.setFont(Font.font(28));
        countdownBox.getChildren().add(countdownLabel);

        Scene countdownScene = new Scene(countdownBox, 400, 300);
        stage.setScene(countdownScene);

        Timeline timeline = new Timeline();
        for (int i = 4; i >= 0; i--) {
            int secondsLeft = i;
            KeyFrame frame = new KeyFrame(Duration.seconds(5 - i), e ->
                    countdownLabel.setText("Starting in " + secondsLeft + "..."));
            timeline.getKeyFrames().add(frame);
        }

        timeline.setOnFinished(e -> {
            // העבר ללוח המשחק כאן
            countdownLabel.setText("Game starts!");
            // TODO: Launch game board
        });

        timeline.play();
    }
    public static void main(String[] args) {
        launch(args);
    }
}
