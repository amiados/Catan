package Model.obj;

import java.time.Instant;
import java.util.UUID;

public class Game {
    private final UUID gameId;
    private final UUID groupId;
    private final Instant createdAt;
    private Instant startedAt;
    private Instant endedAt;
    private final UUID hostId;

    public Game(UUID gameId, UUID groupId, Instant createdAt, Instant startedAt, Instant endedAt, UUID hostId) {
        this.gameId = gameId;
        this.groupId = groupId;
        this.createdAt = createdAt;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.hostId = hostId;
    }

    public UUID getGameId() {
        return gameId;
    }
    public Instant getCreatedAt() {
        return createdAt;
    }
    public Instant getStartedAt() {
        return startedAt;
    }
    public Instant getEndedAt() {
        return endedAt;
    }
    public UUID getGroupId() {
        return groupId;
    }
    public UUID getHostId() {
        return hostId;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }
    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }
}
