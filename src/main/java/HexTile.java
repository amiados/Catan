import javax.swing.*;
import java.awt.*;

public class HexTile {
    private final ResourceType resource; // עץ, חמר, חיטה, וכו'
    private int numberToken; // המספר (2-12)
    private final int q, r;
    private final Image image;

    private Vertex[] vertices = new Vertex[6];
    private Edge[] edges = new Edge[6];

    public HexTile( ResourceType resource, int q, int r) {
        this.resource = resource;
        this.q = q;
        this.r = r;
        this.image = loadImage(resource);
    }

    // getters/setters
    public int getNumberToken() {
        return numberToken;
    }
    public int getQ() {
        return q;
    }
    public int getR() {
        return r;
    }
    public ResourceType getResource() {
        return resource;
    }
    public Image getImage() {
        return image;
    }
    public Vertex getVertex(int index) {
        return vertices[index];
    }
    public Edge getEdge(int index) {
        return edges[index];
    }

    public void setNumberToken(int newNumberToken) {
        this.numberToken = newNumberToken;
    }
    public void setVertex(int index, Vertex v) {
        vertices[index] = v;
    }
    public void setEdge(int index, Edge e) {
        edges[index] = e;
    }

    // methods
    private Image loadImage (ResourceType type) {
        String path = "/Tiles/" + type.name().toLowerCase() + ".png";
        try {
            return new ImageIcon(getClass().getResource(path)).getImage();
        } catch (Exception e) {
            System.err.println("Failed to load image: " + path);
            return null;
        }
    }
    public void draw(Graphics2D g, int centerX, int centerY, int size) {
        int width = size * 2;
        int height = (int) (Math.sqrt(3) * size);
        int drawX = centerX - width / 2;
        int drawY = centerY - height / 2;
        g.drawImage(image, drawX, drawY, width, height, null);
    }
}
