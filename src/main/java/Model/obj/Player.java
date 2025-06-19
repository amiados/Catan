package Model.obj;

import catan.*;

import java.util.*;

public class Player extends User {
    // Public data;
    private final UUID playerId;
    private final UUID userId;
    private PieceColor pieceColor;
    private boolean isTurn;

    // Resources in hand
    private Map<ResourceType, Integer> hand = new EnumMap<>(ResourceType.class);

    // Development cards
    private Map<DevelopmentCard, Integer> availableDevelopmentCards = new HashMap();
    private Map<DevelopmentCard, Integer> newDevelopmentCards = new HashMap();
    private Map<DevelopmentCard, Integer> usedDevelopmentCards = new HashMap();

    // Maximum allowed pieces
    private static final int MAX_SETTLEMENTS = 5;
    private static final int MAX_CITIES = 4;
    private static final int MAX_ROADS = 15;

    // Game status
    private int armySize = 0; // למעקב אחרי חיילים ששוחקו
    private int longestRoad = 0; // למעקב אחרי אורך רציף של דרכים
    private boolean hasLargestArmy = false;
    private boolean hasLongestRoad = false;
    private boolean playedDevelopmentCardThisTurn = false;

    // Structures
    private final Set<Vertex> settlements = new HashSet<>();
    private final Set<Vertex> cities = new HashSet<>();
    private final Set<Edge> roads = new HashSet<>();

    // Points
    private int bonusVictoryPoints = 0;

    // ===== Constructor =====
    public Player(UUID playerId, User user, PieceColor color) {
        super(user.getUsername(), user.getEmail(), user.getPasswordHash());
        this.playerId = playerId;
        this.userId = user.getId();
        this.pieceColor = color;

        for (ResourceType type : ResourceType.values()) {
            hand.put(type, 0);
        }
    }

    // ===== Getters =====

    public UUID getPlayerId() {
        return playerId;
    }
    public PieceColor getPieceColor() {
        return pieceColor;
    }
    public Map<ResourceType, Integer> getHand() {
        return Collections.unmodifiableMap(hand);
    }
    public Map<DevelopmentCard, Integer> getAvailableDevelopmentCards() {
        return availableDevelopmentCards;
    }
    public Map<DevelopmentCard, Integer> getNewDevelopmentCards() {
        return newDevelopmentCards;
    }
    public Map<DevelopmentCard, Integer> getUsedDevelopmentCards() {
        return usedDevelopmentCards;
    }
    public Set<Vertex> getSettlements() {
        return settlements;
    }
    public Set<Vertex> getCities() {
        return cities;
    }
    public Set<Edge> getRoads() {
        return roads;
    }
    public UUID getUserId() {
        return userId;
    }

    // ===== Resource Management =====
    public void addResource(ResourceType type, int amount) {
        hand.put(type, hand.getOrDefault(type, 0) + amount);
    }
    public boolean removeResource(ResourceType type, int amount) {
        int current = hand.getOrDefault(type, 0);
        if(current < amount)
            return false;
        hand.put(type, current - amount);
        return true;
    }
    public boolean hasResources(Map<ResourceType, Integer> cost) {
        for (Map.Entry<ResourceType, Integer> entry : cost.entrySet()) {
            if (hand.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }
    public void removeResources(Map<ResourceType, Integer> cost) {
        cost.forEach(this::removeResource);
    }

    // ===== Development Card Management =====
    public void addDevelopmentCard(DevelopmentCard card) {
        newDevelopmentCards.put(card, newDevelopmentCards.getOrDefault(card, 0) + 1);
    }
    public void promoteNewDevelopmentCards() {
        for (Map.Entry<DevelopmentCard, Integer> entry : newDevelopmentCards.entrySet()) {
            int count = entry.getValue();
            if(count > 0) {
                DevelopmentCard card = entry.getKey();
                availableDevelopmentCards.put(card,
                        availableDevelopmentCards.getOrDefault(card, 0) + count);
            }
        }
        newDevelopmentCards.clear();
    }
    public void markDevCardAsUsed(DevelopmentCard card) {
        int available = availableDevelopmentCards.getOrDefault(card, 0);
        if (available > 0) {
            availableDevelopmentCards.put(card, available - 1);
            usedDevelopmentCards.put(card, usedDevelopmentCards.getOrDefault(card, 0) + 1);
        }
    }

    // ===== Turn Logic =====
    public boolean isTurn() {
        return isTurn;
    }
    public void setTurn(boolean isTurn) {
        this.isTurn = isTurn;
    }
    public void startTurn() {
        playedDevelopmentCardThisTurn = false;
        promoteNewDevelopmentCards();
    }
    public void endTurn() {
        this.isTurn = false;
    }
    public boolean hadPlayedDevelopmentCardThisTurn() {
        return playedDevelopmentCardThisTurn;
    }
    public void setPlayedDevelopmentCardThisTurn(boolean played) {
        this.playedDevelopmentCardThisTurn = played;
    }

    // ===== Building Logic =====
    public boolean canBuildSettlement() {
        return settlements.size() < MAX_SETTLEMENTS;
    }
    public boolean canBuildCity() {
        return cities.size() < MAX_CITIES;
    }
    public boolean canBuildRoad() {
        return roads.size() < MAX_ROADS;
    }
    public void buildSettlement(Vertex location) {
        settlements.add(location);
    }
    public boolean upgradeToCity(Vertex location) {
        if (!settlements.contains(location))
            return false;
        settlements.remove(location);
        cities.add(location);
        return true;
    }
    private void buildRoad(Edge edge) {
        roads.add(edge);
    }

    // ===== Score Logic =====
    private int getVictoryPointsForPlayer() {
        int points = settlements.size() + (cities.size() * 2);

        if (hasLargestArmy)
            points += 2;

        if (hasLongestRoad)
            points +=2;

        return points + bonusVictoryPoints;

    }
    public int getVictoryPointsForOthers() {
        int points = settlements.size() + (cities.size() * 2);

        if (hasLargestArmy)
            points += 2;

        if (hasLongestRoad)
            points +=2;

        return points;

    }
    public int getBonusVictoryPoints() {
        return bonusVictoryPoints;
    }
    private void addBonusVictoryPoint() {
        bonusVictoryPoints++;
    }

    // ===== Army & Road Tracking =====
    public int getArmySize() {
        return armySize;
    }
    public void incrementArmySize() {
        armySize++;
    }
    public int getRoadLength() {
        return longestRoad;
    }
    public boolean hasLargestArmy() {
        return hasLargestArmy;
    }
    public boolean hasLongestRoad() {
        return hasLongestRoad;
    }
}
