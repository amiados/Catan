package catan;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.effect.Blend;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.ColorInput;

public class BuildingFactory {

    public static ImageView creatColoredBuilding(BuildingType type, PieceColor color) {
        if (type == BuildingType.NONE)
            return null;

        Image image = type.getImage();
        ImageView view = new ImageView(image);

        Color coloredImg = mapPieceColorToFXColor(color);

        Blend blend = new Blend(BlendMode.MULTIPLY,
                null,
                new ColorInput(
                        0,
                        0,
                        image.getWidth(),
                        image.getHeight(),
                        coloredImg)
        );
        view.setEffect(blend);
        return view;
    }

    private static Color mapPieceColorToFXColor(PieceColor color) {
        return switch (color) {
            case RED -> Color.RED;
            case BLUE -> Color.DEEPSKYBLUE;
            case WHITE -> Color.LIGHTGRAY;
            case ORANGE -> Color.ORANGE;
            case BLACK -> Color.DARKGRAY;
            case GREEN -> Color.FORESTGREEN;
        };
    }
}
