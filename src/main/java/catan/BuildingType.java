package catan;

import javafx.scene.image.Image;

public enum BuildingType {
    NONE(null),       // אין כלום על הפינה הזו
    SETTLEMENT("GenericBuildings/generic_settlement.png"), // יישוב
    CITY("GenericBuildings/generic_city.png"), // עיר
    ROAD("GenericBuildings/generic_road.png");

    private final String path;
    private final Image image;

    BuildingType(String path) {
        this.path = path;
        if (path != null) {
            this.image = new Image(getClass().getResourceAsStream(path));
        } else {
            this.image = null;
        }
    }

    public String getPath() {
        return path;
    }
    public Image getImage() {
        return image;
    }

}
