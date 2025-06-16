package catan;

import javafx.scene.image.Image;

public enum DevelopmentCard {
    KNIGHT("DevelopmentCards/knight.png"),
    ROAD_BUILDING("DevelopmentCards/roadBuilding.png"),
    YEAR_OF_PLENTY("DevelopmentCards/yearOfPlenty.png"),
    MONOPOLY("DevelopmentCards/monopoly.png"),
    VICTORY_POINT("DevelopmentCards/victoryPoint.png");

    private final String path;
    private final Image image;

    DevelopmentCard(String path) {
        this.path = path;

        if (path != null) {
            this.image = new Image(getClass().getResourceAsStream(path));
        } else {
            this.image = null;
        }
    }

    public Image getImage() {
        return image;
    }
    public String getPath() {
        return path;
    }

}
