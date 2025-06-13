package catan;

public enum ResourceType {
    WHEAT("wheat.png"),
    WOOD("wood.png"),
    SHEEP("sheep.png"),
    BRICK("brick.png"),
    ORE("ore.png"),
    DESERT("desert.png");

    private final String imagePath;

    ResourceType(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getImagePath() { return imagePath; }
}
