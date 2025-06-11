import javax.swing.*;
import java.util.*;

public class CatanGame {
    public static void main(String[] args) {
        List<HexTile> tiles = new ArrayList<>();

        ResourceType[] resources = {
                // שורה 0: 7 אריחי מים
                ResourceType.WATER, ResourceType.WATER, ResourceType.WATER, ResourceType.WATER, ResourceType.WATER, ResourceType.WATER, ResourceType.WATER,
                // שורה 1: 8 אריחים (1 מים, 6 פנימיים, 1 מים)
                ResourceType.WATER, ResourceType.WOOD, ResourceType.BRICK, ResourceType.SHEEP, ResourceType.ORE, ResourceType.WHEAT, ResourceType.DESERT, ResourceType.WATER,
                // שורה 2: 9 אריחים (2 מים, 5 פנימיים, 2 מים)
                ResourceType.WATER, ResourceType.WATER, ResourceType.WOOD, ResourceType.BRICK, ResourceType.SHEEP, ResourceType.ORE, ResourceType.WHEAT, ResourceType.DESERT, ResourceType.WATER,
                // שורה 3: 10 אריחים (3 מים, 4 פנימיים, 3 מים)
                ResourceType.WATER, ResourceType.WATER, ResourceType.WATER, ResourceType.WOOD, ResourceType.BRICK, ResourceType.SHEEP, ResourceType.ORE, ResourceType.WHEAT, ResourceType.DESERT, ResourceType.WATER,
                // שורה 4: 9 אריחים, מראה סימטרי
                ResourceType.WATER, ResourceType.WATER, ResourceType.WOOD, ResourceType.BRICK, ResourceType.SHEEP, ResourceType.ORE, ResourceType.WHEAT, ResourceType.DESERT, ResourceType.WATER,
                // שורה 5: 8
                ResourceType.WATER, ResourceType.WOOD, ResourceType.BRICK, ResourceType.SHEEP, ResourceType.ORE, ResourceType.WHEAT, ResourceType.DESERT, ResourceType.WATER,
                // שורה 6: 7
                ResourceType.WATER, ResourceType.WATER, ResourceType.WATER, ResourceType.WATER, ResourceType.WATER, ResourceType.WATER, ResourceType.WATER
        };

        for (ResourceType res : resources) {
            tiles.add(new HexTile(res, 0, 0));
        }

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Catan Board");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new Board(tiles));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
