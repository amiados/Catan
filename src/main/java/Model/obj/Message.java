package Model.obj;

import java.time.Instant;
import java.util.UUID;

public class Message {
    private final UUID id;
    private final UUID gameId;
    private final UUID senderId;
    private final byte[] content;
    private final Instant sentAt;

    public Message(UUID id, UUID gameId, UUID senderId, byte[] content, Instant sentAt) {
        this.id = id;
        this.gameId = gameId;
        this.senderId = senderId;
        this.content = content;
        this.sentAt = sentAt;
    }


    public UUID getId() {
        return id;
    }
    public UUID getGameId() {
        return gameId;
    }
    public UUID getSenderId() {
        return senderId;
    }
    public byte[] getContent() {
        return content;
    }
    public Instant getSentAt() {
        return sentAt;
    }
}
