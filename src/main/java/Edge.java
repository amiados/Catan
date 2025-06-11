public class Edge {
    private Player owner;
    private Vertex vertexA;
    private Vertex vertexB;

    public Edge(Player owner, Vertex vertexA, Vertex vertexB) {
        this.owner = owner;
        this.vertexA = vertexA;
        this.vertexB = vertexB;
    }

    // getters/setters
    public Player getOwner() {
        return owner;
    }
    public Vertex getVertexA() {
        return vertexA;
    }
    public Vertex getVertexB() {
        return vertexB;
    }
    public void setOwner(Player owner) {
        this.owner = owner;
    }
    public void setVertexA(Vertex vertexA) {
        this.vertexA = vertexA;
    }
    public void setVertexB(Vertex vertexB) {
        this.vertexB = vertexB;
    }
}
