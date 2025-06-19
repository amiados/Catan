package Model.obj;

import Model.InviteStatus;

import java.time.Instant;
import java.util.UUID;

public class Invite {
    private final UUID id;
    private final UUID groupId;
    private final UUID senderId;
    private final UUID receiverId;
    private InviteStatus status;
    private final Instant sentAt;

    public Invite(UUID id, UUID groupId, UUID senderId, UUID receiverId, InviteStatus status, Instant sentAt) {
        this.id = id;
        this.groupId = groupId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.status = status;
        this.sentAt = sentAt;
    }

    public UUID getId() {
        return id;
    }
    public UUID getGroupId() {
        return groupId;
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
