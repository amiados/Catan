package catan;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.security.SecureRandom;
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

    // מעקב אחר האלמנט שנבחר (catan.Vertex או catan.Edge)
    private Circle selectedVertex = null;
    private Line selectedEdge = null;


    @Override
    public void start(Stage stage) {
        root = new Pane();
        root.setPrefSize(BOARD_WIDTH, BOARD_HEIGHT);
        root.setStyle("-fx-background-color: #4682B4;"); // רקע כחול כמו הים

        initializeTiles();
        createBoard();
        initializeResourcesCards();
        initializeAction();

        Scene scene = new Scene(root, BOARD_WIDTH, BOARD_HEIGHT);
        stage.setTitle("Catan catan.Board");
        stage.setScene(scene);
        stage.show();

        root.setOnMouseClicked(event -> {
            System.out.println("Mouse clicked at: X = " + event.getX() + ", Y = " + event.getY());
        });
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
        Collections.shuffle(shuffledNumbers, new SecureRandom());
        Collections.shuffle(resources, new SecureRandom());

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

    private void initializeResourcesCards() {
        String[] resources = {"wheatCard", "woodCard", "sheepCard", "brickCard", "oreCard", "developmentCard"};
        double x = BOARD_WIDTH / 9;
        double y = BOARD_HEIGHT - 50;
        double spacing = 10;

        for (String name : resources) {
            try {
                Image img = new Image(getClass().getResourceAsStream("/ResourceCards/" + name + ".png"));
                ImageView view = new ImageView(img);
                view.setX(x);
                view.setY(y);
                view.setFitWidth(100); // ניתן לשנות בהתאם לגודל אחיד
                view.setFitHeight(140);
                view.setPreserveRatio(true);

                root.getChildren().add(view);

                x += view.getFitWidth() + spacing;
            } catch (Exception e) {
                System.out.println("Couldn't load image for: " + name);
            }
        }
        try {
            Image img = new Image(getClass().getResourceAsStream("/Utils/buildingCost.png"));
            ImageView view = new ImageView(img);
            view.setX(10);
            view.setY(410);
            view.setFitWidth(230);
            view.setFitHeight(220);
            view.setPreserveRatio(false);

            root.getChildren().add(view);
        }  catch (Exception e) {
            System.out.println("Couldn't load image for: buildingCost");
        }

    }

    private void initializeAction() {

        String[] actions = {"generic_road", "generic_settlement", "generic_city"};
        VBox actionBox = new VBox(9);
        actionBox.setLayoutX(10);
        actionBox.setLayoutY(10);
        actionBox.setPrefWidth(80);
        actionBox.setAlignment(Pos.TOP_CENTER);
        actionBox.setStyle("""
         -fx-background-color: linear-gradient(#f5deb3, #e0c189);
            -fx-background-radius: 25;
            -fx-border-color: #5b3a1e;
            -fx-border-width: 4;
            -fx-border-radius: 25;
            -fx-padding: 15;
            -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.5), 10, 0.3, 2, 2);
        """);

        Text title = new Text("✦ ACTIONS ✦");
        title.setFont(Font.font("Papyrus", FontWeight.BOLD, 16));
        title.setFill(Color.DARKRED);
        title.setEffect(new DropShadow(2, Color.BLACK));
        title.setTextAlignment(TextAlignment.CENTER);
        actionBox.getChildren().add(title);

        for (String action : actions) {
            try {
                Image image = new Image(getClass().getResourceAsStream("/GenericBuildings/" + action + ".png"));
                ImageView view = new ImageView(image);
                view.setFitWidth(50);
                view.setFitHeight(50);

                StackPane wrapper = new StackPane(view);
                wrapper.setPrefSize(60, 60);
                wrapper.setMaxSize(60, 60);
                wrapper.setMinSize(60, 60);
                wrapper.setAlignment(Pos.CENTER);
                wrapper.setStyle("""
                    -fx-background-color: #fff5e1;
                    -fx-border-color: #6e4b1f;
                    -fx-border-width: 2;
                    -fx-border-radius: 10;
                    -fx-background-radius: 10;
                """);

                // שם מתחת לתמונה (לפי הצורך)
                String label = switch (action) {
                    case "generic_road" -> "Road";
                    case "generic_settlement" -> "Settlement";
                    case "generic_city" -> "City";
                    default -> "";
                };

                Text caption = new Text(label);
                caption.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 12));
                caption.setFill(Color.SADDLEBROWN);

                VBox item = new VBox(5, wrapper, caption);
                item.setAlignment(Pos.CENTER);

                actionBox.getChildren().add(item);

            } catch (Exception e) {
                System.out.println("Couldn't load image for: " + action);
                e.printStackTrace();
            }
        }
        root.getChildren().add(actionBox);

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
        hex.setStrokeWidth(10);

        // הוספת המשושה
        root.getChildren().add(hex);

        // הוספת קודקודים וצלעות עם תגובת עכבר
        List<double[]> vertices = new ArrayList<>();
        for (int i=0; i<6; i++) {
            double angle = Math.toRadians(60 * i - 30);
            double x = tile.getX() + HEX_SIZE * Math.cos(angle);
            double y = tile.getY() + HEX_SIZE * Math.sin(angle);
            vertices.add(new double[]{x, y});

            // יצירת קודקוד
            Circle verexCircle = new Circle(x, y, 6, Color.GRAY);

            DropShadow glow = new DropShadow();
            glow.setRadius(15);
            glow.setColor(Color.AQUA);

            verexCircle.setOnMouseEntered(e -> {
                if (verexCircle != selectedVertex) {
                    verexCircle.setFill(Color.YELLOW);
                }
            });

            verexCircle.setOnMouseExited(e -> {
                if (verexCircle != selectedVertex) {
                    verexCircle.setFill(Color.GRAY);
                }
            });

            verexCircle.setOnMouseClicked(e -> {

                // הסרת בחירה מקודקוד קודם
                if (selectedVertex != null) {
                   selectedVertex.setFill(Color.GRAY);
                   selectedVertex.setFill(null);
                }

                // הסרת בחירה מצלע קודמת
                if (selectedEdge != null) {
                    selectedEdge.setStroke(Color.GRAY);
                    selectedEdge.setEffect(null);
                }

                selectedVertex = verexCircle;
                verexCircle.setFill(Color.BLUE);
                verexCircle.setEffect(glow);
                verexCircle.setFill(Color.BLUE);
            });

            root.getChildren().add(verexCircle);
        }
        // יצירת צלעות (קווים בין כל זוג קודקודים סמוכים)
        for (int i=0; i<6; i++) {
            double[] p1 = vertices.get(i);
            double[] p2 = vertices.get((i+1) % 6);
            Line edgeLine = new Line(p1[0], p1[1], p2[0], p2[1]);
            edgeLine.setStroke(Color.GRAY);
            edgeLine.setStrokeWidth(3);

            DropShadow glow = new DropShadow();
            glow.setRadius(20);
            glow.setColor(Color.ORANGE);

            edgeLine.setOnMouseEntered(e -> {
                if (edgeLine != selectedEdge)
                    edgeLine.setStroke(Color.YELLOW);
            });

            edgeLine.setOnMouseExited(e -> {
                if (edgeLine != selectedEdge)
                    edgeLine.setStroke(Color.GRAY);
            });

            edgeLine.setOnMouseClicked(e -> {
                // הסרת בחירה מצלע קודמת
                if (selectedEdge != null) {
                    selectedEdge.setStroke(Color.GRAY);
                    selectedEdge.setEffect(null);
                }
                // הסרת בחירה מקודקוד קודם
                if (selectedVertex != null) {
                    selectedVertex.setFill(Color.LIGHTGRAY);
                    selectedVertex.setEffect(null);
                }

                selectedEdge = edgeLine;
                edgeLine.setStroke(Color.RED);
                edgeLine.setEffect(glow);
            });

            root.getChildren().add(edgeLine);
        }

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

    private Rectangle fdasf(double x, double y, double width, double height) {
        Rectangle rectangle = new Rectangle(x, y, width, height);
        rectangle.setArcHeight(30);
        rectangle.setArcWidth(20);
        return rectangle;
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
