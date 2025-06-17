package Model.obj;

import Model.InviteStatus;

import java.time.Instant;
import java.util.UUID;

public class Invite {
    private UUID id;
    private UUID gameId;
    private UUID senderId;
    private UUID receiverId;
    private InviteStatus status;
    private Instant sentAt;

    public Invite(UUID id, UUID gameId, UUID senderId, UUID receiverId, InviteStatus status, Instant sentAt) {
        this.id = id;
        this.gameId = gameId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.status = status;
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
    public UUID getReceiverId() {
        return receiverId;
    }
    public InviteStatus getStatus() {
        return status;
    }
    public Instant getSentAt() {
        return sentAt;
    }

    public void setStatus(InviteStatus status) {
        this.status = status;
    }
}
