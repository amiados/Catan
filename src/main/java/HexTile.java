import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Polygon;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.Group;

public class HexTile {
    private final ResourceType resourceType;
    private int number;
    private double x, y;

    public HexTile(ResourceType resourceType, int number, double x, double y) {
        this.resourceType = resourceType;
        this.number = number;
        this.x = x;
        this.y = y;
    }

    // getters
    public int getNumber() {
        return number;
    }
    public ResourceType getResourceType() {
        return resourceType;
    }
    public double getX() {
        return x;
    }
    public double getY() {
        return y;
    }

    // setters
    public void setNumber(int number) {
        this.number = number;
    }
    public void setX(double x) {
        this.x = x;
    }
    public void setY(double y) {
        this.y = y;
    }

}
