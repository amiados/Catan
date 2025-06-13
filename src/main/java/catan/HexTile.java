package catan;

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
