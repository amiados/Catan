package catan;

import Model.OBJ.Player;

public class Edge {
    private Player owner;
    private final Vertex vertexA;
    private final Vertex vertexB;

    public Edge(Player owner, Vertex vertexA, Vertex vertexB) {
        if (vertexA.getID() > vertexB.getID()) {
            swap(vertexA, vertexB);
        }
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

    // === Methods ===
    private void swap(Vertex a, Vertex b) {
        Vertex v = a;
        a = b;
        b = v;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof Edge))
            return false;
        Edge other = (Edge) obj;

        return (vertexA.equals(other.vertexA) && vertexB.equals(other.vertexB));
    }

    @Override
    public int hashCode() {
        return vertexA.hashCode() ^ vertexB.hashCode();
    }

}
