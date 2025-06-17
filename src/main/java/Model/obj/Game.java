package Model.obj;

import Model.GameStatus;

import java.time.Instant;
import java.util.UUID;

public class Game {
    private UUID id;
    private Instant createdAt;
    private Instant startedAt;
    private Instant endedAt;
    private UUID userId;
    private GameStatus status;

    public Game(UUID id, Instant createdAt, Instant startedAt, Instant endedAt, UUID userId, GameStatus status) {
        this.id = id;
        this.createdAt = createdAt;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.userId = userId;
        this.status = status;
    }

    public UUID getId() {
        return id;
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
    public UUID getUserId() {
        return userId;
    }
    public GameStatus getStatus() {
        return status;
    }

    public void setId(UUID id) {
        this.id = id;
    }
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }
    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }
    public void setUserId(UUID userId) {
        this.userId = userId;
    }
    public void setStatus(GameStatus status) {
        this.status = status;
    }
}
