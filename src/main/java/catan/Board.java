package catan;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Board extends Application {
    private static final double HEX_SIZE = 70;
    private static final double BOARD_WIDTH = 900;
    private static final double BOARD_HEIGHT = 700;

    private Pane root;
    private List<HexTile> tiles;

    // רשימת המספרים
    private final List<Integer> numbers = List.of(2, 3, 3, 4, 4, 5, 5, 6, 6, 8, 8, 9, 9, 10, 10, 11, 11, 12);

    @Override
    public void start(Stage stage) {
        root = new Pane();
        root.setPrefSize(BOARD_WIDTH, BOARD_HEIGHT);
        root.setStyle("-fx-background-color: #4682B4;"); // רקע כחול כמו הים

        initializeTiles();
        createBoard();

        Scene scene = new Scene(root, BOARD_WIDTH, BOARD_HEIGHT);
        stage.setTitle("Catan catan.Board");
        stage.setScene(scene);
        stage.show();
    }

    private void initializeTiles() {
        tiles = new ArrayList<>();

        // יצירת רשימת המשאבים והמספרים כמו במשחק האמיתי
        List<ResourceType> resources = new ArrayList<>();

        // 4 חיטה, 4 עץ, 4 כבשים
        for (int i=0; i<4; i++) {
            resources.add(ResourceType.WHEAT);
            resources.add(ResourceType.WOOD);
            resources.add(ResourceType.SHEEP);
        }

        // 3 לבנים, 3 עפר
        for (int i=0; i<3; i++) {
            resources.add(ResourceType.BRICK);
            resources.add(ResourceType.ORE);
        }

        // 1 מדבר
        resources.add(ResourceType.DESERT);

        // רשימת המספרים
        List<Integer> shuffledNumbers = new ArrayList<>(numbers);
        Collections.shuffle(shuffledNumbers);
        Collections.shuffle(resources);

        // יצירת הלוח - 19 משבצות במבנה משושה
        double centerX = BOARD_WIDTH / 2;
        double centerY = BOARD_HEIGHT / 2;

        // מרחקים מדויקים למשושים צמודים
        double hexWidth = HEX_SIZE * Math.sqrt(3); // רוחב המשושה
        double hexVerticalSpacing = HEX_SIZE * 1.5; // מרווח אנכי בין שורות

        // מערך המיקומים של כל 19 המשבצות - מחושב בדיוק לצמידות
        double[][] positions = {
                // שורה עליונה (3 משבצות)
                {centerX - hexWidth/2, centerY - hexVerticalSpacing * 2},
                {centerX + hexWidth/2, centerY - hexVerticalSpacing * 2},
                {centerX + hexWidth * 1.5, centerY - hexVerticalSpacing * 2},

                // שורה שנייה (4 משבצות)
                {centerX - hexWidth, centerY - hexVerticalSpacing},
                {centerX, centerY - hexVerticalSpacing},
                {centerX + hexWidth, centerY - hexVerticalSpacing},
                {centerX + hexWidth * 2, centerY - hexVerticalSpacing},

                // שורה אמצעית (5 משבצות)
                {centerX - hexWidth * 1.5, centerY},
                {centerX - hexWidth/2, centerY},
                {centerX + hexWidth/2, centerY}, // מרכז
                {centerX + hexWidth * 1.5, centerY},
                {centerX + hexWidth * 2.5, centerY},

                // שורה רביעית (4 משבצות)
                {centerX - hexWidth, centerY + hexVerticalSpacing},
                {centerX, centerY + hexVerticalSpacing},
                {centerX + hexWidth, centerY + hexVerticalSpacing},
                {centerX + hexWidth * 2, centerY + hexVerticalSpacing},

                // שורה תחתונה (3 משבצות)
                {centerX - hexWidth/2, centerY + hexVerticalSpacing * 2},
                {centerX + hexWidth/2, centerY + hexVerticalSpacing * 2},
                {centerX + hexWidth * 1.5, centerY + hexVerticalSpacing * 2}
        };

        int numberIndex = 0;
        for (int i = 0; i < positions.length && i < resources.size(); i++) {
            ResourceType resource = resources.get(i);
            int number = resource == ResourceType.DESERT ? 0 : shuffledNumbers.get(numberIndex++);
            tiles.add(new HexTile(resource, number, positions[i][0], positions[i][1]));
        }
    }

    private void createBoard() {
        for (HexTile tile : tiles) {
            createHexTile(tile);
        }
    }

    private void createHexTile(HexTile tile) {
        // יצירת המשושה
        Polygon hex = createHexagon(tile.getX(), tile.getY(), HEX_SIZE);

        // טעינת תמונת המשאב ומילוי המשושה בה
        try {
            String imgPath = "/Tiles/" + tile.getResourceType().getImagePath();
            Image image = new Image(getClass().getResourceAsStream(imgPath));

            if (!image.isError()) {
                ImagePattern pattern = new ImagePattern(
                        image,
                        tile.getX() - HEX_SIZE,
                        tile.getY() - HEX_SIZE,
                        HEX_SIZE * 2,
                        HEX_SIZE * 2,
                        false
                );
                // מילוי המשושה בתמונה במקום צבע
                hex.setFill(pattern);
            } else {
                // אם התמונה לא נמצאת, נשתמש בצבע רקע
                hex.setFill(getResourceColor(tile.getResourceType()));
            }

        } catch (Exception e) {
            // אם התמונה לא נמצאת, נשתמש בצבע רקע
            hex.setFill(getResourceColor(tile.getResourceType()));
            System.out.println("Could not load image for: " + tile.getResourceType().getImagePath());
        }

        // עיצוב קווי המשושה
        hex.setStroke(Color.BLACK);
        hex.setStrokeWidth(6);

        // הוספת המשושה
        root.getChildren().add(hex);

        // הוספת מספר אם זה לא מדבר
        if (tile.getNumber() > 0) {
            // עיגול רקע למספר
            Circle numberCircle = new Circle(tile.getX(), tile.getY(), 20);
            numberCircle.setFill(Color.WHITE);
            numberCircle.setStroke(Color.BLACK);
            numberCircle.setStrokeWidth(2);

            // טקסט המספר
            Text numberText = new Text(String.valueOf(tile.getNumber()));
            numberText.setFont(Font.font("Arial", FontWeight.BOLD, 22));

            // צבע אדום למספרים 6 ו-8 (סיכוי גבוה)
            if (tile.getNumber() == 6 || tile.getNumber() == 8) {
                numberText.setFill(Color.RED);
            } else {
                numberText.setFill(Color.BLACK);
            }

            // מרכוז הטקסט
            numberText.setX(tile.getX() - numberText.getBoundsInLocal().getWidth() / 2);
            numberText.setY(tile.getY() + numberText.getBoundsInLocal().getHeight() / 4);

            root.getChildren().addAll(numberCircle, numberText);
        }
    }

    private Polygon createHexagon(double centerX, double centerY, double size) {
        Polygon hexagon = new Polygon();

        for(int i=0; i<6; i++) {
            double angle = Math.PI / 2 + i * Math.PI / 3; // התחלה מהחלק העליון
            double x = centerX + size * Math.cos(angle);
            double y = centerY + size * Math.sin(angle);
            hexagon.getPoints().addAll(x, y);
        }

        return hexagon;
    }

    // פונקציה עזר לצבעי רקע אם התמונות לא נטענות
    private Color getResourceColor(ResourceType resourceType) {
        switch (resourceType) {
            case WHEAT: return Color.GOLD;
            case WOOD: return Color.DARKGREEN;
            case SHEEP: return Color.LIGHTGREEN;
            case BRICK: return Color.DARKRED;
            case ORE: return Color.GRAY;
            case DESERT: return Color.SANDYBROWN;
            default: return Color.BEIGE;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
