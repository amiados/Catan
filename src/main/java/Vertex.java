import catan.BuildingType;

public class Vertex {
    private final int ID;
    private BuildingType building;
    private Player owner;

    public Vertex(int ID, BuildingType building, Player owner) {

        this.ID = ID;
        this.building = building;
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

}
