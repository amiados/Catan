package Model.obj;

import java.time.Instant;
import java.util.UUID;

public class Message {
    private UUID id;
    private UUID gameId;
    private UUID senderId;
    private byte[] content;
    private Instant sentAt;

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

    public void setContent(byte[] content) {
        this.content = content;
    }
}
