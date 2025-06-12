import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.Font;

public class Board extends Application { private static final double SIZE = 50;
    private static final double HEX_WIDTH = SIZE * Math.sqrt(3);
    private static final double HEX_HEIGHT = SIZE * 2;

    // פריסת הלוח הנכונה של קטאן
    private static final int[][] BOARD_LAYOUT = {
            {-1, -1, -1,  0,  0,  0, -1, -1, -1},
            {-1, -1,  0,  1,  1,  1,  0, -1, -1},
            {-1,  0,  1,  1,  1,  1,  1,  0, -1},
            { 0,  1,  1,  1,  1,  1,  1,  0, -1},
            {-1,  0,  1,  1,  1,  1,  1,  0, -1},
            {-1, -1,  0,  1,  1,  1,  0, -1, -1},
            {-1, -1, -1,  0,  0,  0, -1, -1, -1}
    };

    // משאבים כפי שהם מופיעים בלוח הרגיל
    private static final ResourceType[] LAND_RESOURCES = {
            // שורה 1 (3 אריחים)
            ResourceType.ORE, ResourceType.SHEEP, ResourceType.WOOD,
            // שורה 2 (4 אריחים)
            ResourceType.WHEAT, ResourceType.BRICK, ResourceType.SHEEP, ResourceType.BRICK,
            // שורה 3 (5 אריחים)
            ResourceType.WHEAT, ResourceType.WOOD, ResourceType.DESERT, ResourceType.WOOD, ResourceType.ORE,
            // שורה 4 (4 אריחים)
            ResourceType.BRICK, ResourceType.ORE, ResourceType.WHEAT, ResourceType.SHEEP,
            // שורה 5 (3 אריחים)
            ResourceType.WOOD, ResourceType.SHEEP, ResourceType.WHEAT
    };

    // מספרי קוביות (ללא המדבר)
    private static final int[] DICE_NUMBERS = {
            10, 2, 9,
            12, 6, 4, 10,
            9, 11, 3, 8,
            8, 3, 4, 5,
            5, 6, 11
    };

    // מיקומי נמלים
    private static final String[] PORTS = {"3:1", "WOOD", "3:1", "BRICK", "3:1", "SHEEP", "3:1", "ORE", "3:1"};

    @Override
    public void start(Stage stage) {
        Pane root = new Pane();
        root.setStyle("-fx-background-color: #87CEEB;"); // צבע רקע כחול בהיר

        // מרכז הלוח
        double centerX = 500;
        double centerY = 400;

        int landIndex = 0;
        int diceIndex = 0;

        // יצירת הלוח
        for (int row = 0; row < BOARD_LAYOUT.length; row++) {
            for (int col = 0; col < BOARD_LAYOUT[row].length; col++) {
                int tileType = BOARD_LAYOUT[row][col];
                if (tileType == -1) continue;

                // חישוב מיקום
                double x = centerX + (col - 4) * HEX_WIDTH * 0.75;
                double y = centerY + (row - 3) * HEX_HEIGHT * 0.75;

                // התאמה לשורות זוגיות
                if (col % 2 == 1) {
                    y += HEX_HEIGHT * 0.375;
                }

                if (tileType == 0) { // מים
                    HexTile waterTile = new HexTile(ResourceType.WATER, SIZE, 0);
                    waterTile.setLayoutX(x);
                    waterTile.setLayoutY(y);
                    root.getChildren().add(waterTile);
                } else if (tileType == 1) { // יבשה
                    ResourceType resource = LAND_RESOURCES[landIndex];
                    int diceNumber = 0;

                    if (resource != ResourceType.DESERT) {
                        diceNumber = DICE_NUMBERS[diceIndex++];
                    }

                    HexTile landTile = new HexTile(resource, SIZE, diceNumber);
                    landTile.setLayoutX(x);
                    landTile.setLayoutY(y);

                    // אפקט hover
                    landTile.setOnMouseEntered(e -> {
                        landTile.setScaleX(1.1);
                        landTile.setScaleY(1.1);
                    });
                    landTile.setOnMouseExited(e -> {
                        landTile.setScaleX(1.0);
                        landTile.setScaleY(1.0);
                    });

                    root.getChildren().add(landTile);
                    landIndex++;
                }
            }
        }

        // הוספת נמלים במיקומים המתאימים
        addPorts(root, centerX, centerY);

        // כותרת
        Text title = new Text(20, 40, "Settlers of Catan - קטאן");
        title.setFont(Font.font("Arial", 28));
        title.setFill(Color.DARKBLUE);
        root.getChildren().add(title);

        // מקרא
        addLegend(root);

        Scene scene = new Scene(root, 1000, 800);
        stage.setScene(scene);
        stage.setTitle("Catan Board - לוח קטאן");
        stage.show();
    }

    private void addPorts(Pane root, double centerX, double centerY) {
        // מיקומי נמלים סביב הלוח
        double[][] portPositions = {
                {centerX - 150, centerY - 200, 0},      // למעלה שמאל
                {centerX, centerY - 220, 0},            // למעלה
                {centerX + 150, centerY - 200, 60},     // למעלה ימין
                {centerX + 200, centerY, 120},          // ימין
                {centerX + 150, centerY + 200, 180},    // למטה ימין
                {centerX, centerY + 220, 180},          // למטה
                {centerX - 150, centerY + 200, 240},    // למטה שמאל
                {centerX - 200, centerY, 300},          // שמאל
                {centerX - 200, centerY - 100, 300}     // שמאל למעלה
        };

        for (int i = 0; i < Math.min(PORTS.length, portPositions.length); i++) {
            Port port = new Port(PORTS[i], SIZE * 0.6);
            port.setLayoutX(portPositions[i][0]);
            port.setLayoutY(portPositions[i][1]);
            port.setRotate(portPositions[i][2]);
            root.getChildren().add(port);
        }
    }

    private void addLegend(Pane root) {
        Text legendTitle = new Text(20, 100, "משאבים:");
        legendTitle.setFont(Font.font("Arial", 16));
        legendTitle.setFill(Color.DARKBLUE);
        root.getChildren().add(legendTitle);

        String[] resourceNames = {"עץ", "חמר", "כבשים", "חיטה", "עפרות", "מדבר"};
        Color[] resourceColors = {
                Color.web("#228B22"), Color.web("#CD853F"), Color.web("#90EE90"),
                Color.web("#FFD700"), Color.web("#708090"), Color.web("#F4A460")
        };

        for (int i = 0; i < resourceNames.length; i++) {
            Circle colorCircle = new Circle(35, 130 + i * 25, 8);
            colorCircle.setFill(resourceColors[i]);
            colorCircle.setStroke(Color.BLACK);

            Text resourceName = new Text(50, 135 + i * 25, resourceNames[i]);
            resourceName.setFont(Font.font("Arial", 12));
            resourceName.setFill(Color.BLACK);

            root.getChildren().addAll(colorCircle, resourceName);
        }

        // הוראות
        Text instructions = new Text(20, 300,
                "חוקי המשחק:\n" +
                        "• מספרים אדומים (6,8) - יותר סיכויים\n" +
                        "• נמלים מאפשרים סחר\n" +
                        "• המדבר מתחיל עם השודד");
        instructions.setFont(Font.font("Arial", 11));
        instructions.setFill(Color.DARKBLUE);
        root.getChildren().add(instructions);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
