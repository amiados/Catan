import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Polygon;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.Group;

public class HexTile extends Group{
    private final ResourceType resource;
    private final Image image;
    private final Polygon hexagon;
    private final int diceNumber;
    private Circle numberCircle;
    private Text numberText;

    public HexTile(ResourceType resource, double size, int diceNumber) {
        this.resource = resource;
        this.diceNumber = diceNumber;
        this.image = loadImage(resource);
        this.hexagon = new Polygon();

        createHexagon(size);
        if (diceNumber > 0 && resource != ResourceType.DESERT) {
            createNumberToken(size);
        }
    }

    // getters/setters
    public ResourceType getResource() {
        return resource;
    }
    public Image getImage() {
        return image;
    }
    public int getDiceNumber() {
        return diceNumber;
    }
    // methods
    private Image loadImage(ResourceType type) {
        String path = "/Tiles/" + type.name().toLowerCase() + ".png";
        try {
            return new Image(getClass().getResourceAsStream(path));
        } catch (Exception e) {
            System.err.println("Could not load " + path);
            return null;
        }
    }

    private void createHexagon(double size) {
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(60 * i);
            double x = size * Math.cos(angle);
            double y = size * Math.sin(angle);
            hexagon.getPoints().addAll(x, y);
        }

        hexagon.setFill(new ImagePattern(image));

        hexagon.setStroke(Color.BLACK);
        hexagon.setStrokeWidth(2);
        getChildren().add(hexagon);
    }

    private void createNumberToken(double size) {
        // יצירת עיגול למספר הקובייה
        numberCircle = new Circle(size * 0.3);
        numberCircle.setFill(Color.BEIGE);
        numberCircle.setStroke(Color.BLACK);
        numberCircle.setStrokeWidth(2);

        // טקסט המספר
        numberText = new Text(String.valueOf(diceNumber));
        numberText.setFont(Font.font("Arial", size * 0.4));
        numberText.setFill(diceNumber == 6 || diceNumber == 8 ? Color.RED : Color.BLACK);

        // מרכוז הטקסט
        numberText.setX(-numberText.getBoundsInLocal().getWidth() / 2);
        numberText.setY(numberText.getBoundsInLocal().getHeight() / 4);

        getChildren().addAll(numberCircle, numberText);
    }
}
