import javax.swing.*;
import java.awt.*;
import java.util.List;

public class Board extends JPanel {
    private static final int HEX_SIZE   = 60;
    private static final int HEX_WIDTH  = HEX_SIZE * 2;
    private static final int HEX_HEIGHT = (int)(Math.sqrt(3) * HEX_SIZE);
    private static final int X_SPACING  = (int)(HEX_SIZE * 1.5);
    private static final int Y_SPACING  = HEX_HEIGHT;

    private final List<HexTile> tiles;

    public Board(List<HexTile> tiles) {
        this.tiles = tiles;
        setPreferredSize(new Dimension(900, 900));
        setBackground(Color.WHITE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        int[] rowLens = {7,8,9,10,9,8,7};
        int idx = 0;
        int totalH = rowLens.length * Y_SPACING;
        int startY = getHeight()/2 - totalH/2 + Y_SPACING/2;

        for (int row = 0; row < rowLens.length; row++) {
            int len = rowLens[row];
            int rowW = HEX_WIDTH + (len-1)*X_SPACING;
            int baseX = getWidth()/2 - rowW/2 + HEX_WIDTH/2;
            int offset = (row % 2 == 1) ? HEX_SIZE : 0;
            int y = startY + row * Y_SPACING;

            for (int col = 0; col < len; col++) {
                HexTile tile = tiles.get(idx++);
                int x = baseX + offset + col * X_SPACING;
                tile.draw(g2, x, y, HEX_SIZE);
            }
        }
    }
}
