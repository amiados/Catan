import javafx.scene.shape.Polygon;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.Group;

public class Port extends Group {
    private final String portType;
    private final Polygon portShape;
    private Text portText;

    public Port(String portType, double size) {
        this.portType = portType;
        this.portShape = new Polygon();
        createPort(size);
    }

    private void createPort(double size) {
        // יצירת צורת הנמל (משולש או חצי משושה)
        double[] points = {
                -size * 0.3, -size * 0.5,
                size * 0.3, -size * 0.5,
                size * 0.5, 0,
                size * 0.3, size * 0.5,
                -size * 0.3, size * 0.5,
                -size * 0.5, 0
        };

        portShape.getPoints().addAll(
                points[0], points[1],
                points[2], points[3],
                points[4], points[5]
        );

        portShape.setFill(Color.LIGHTBLUE);
        portShape.setStroke(Color.DARKBLUE);
        portShape.setStrokeWidth(2);

        // טקסט הנמל
        portText = new Text(portType);
        portText.setFont(Font.font("Arial", size * 0.15));
        portText.setFill(Color.DARKBLUE);

        portText.setX(-portText.getBoundsInLocal().getWidth() / 2);
        portText.setY(portText.getBoundsInLocal().getHeight() / 4);

        getChildren().addAll(portShape, portText);
    }
}
