package catan;

import Model.OBJ.Player;

public class Vertex {
    private final int ID;
    private BuildingType building;
    private Player owner;

    public Vertex(int ID, BuildingType building, Player owner) {
        this.ID = ID;
        this.building = building;
        this.owner = owner;
    }

    // getters/setters
    public BuildingType getBuilding() {
        return building;
    }
    public int getID() {
        return ID;
    }
    public Player getOwner() {
        return owner;
    }
    public void setBuilding(BuildingType building) {
        this.building = building;
    }
    public void setOwner(Player owner) {
        this.owner = owner;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof Vertex))
            return false;
        Vertex other = (Vertex) obj;
        return ID == other.getID();
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(ID);
    }
}
