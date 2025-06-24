package Model.OBJ;

import java.time.Instant;
import java.util.UUID;

/**
 * בדיוק כמו GameMessage, רק שהשדה השני הוא groupId במקום gameId
 */
public class GroupMessage {
    private final UUID id;
    private final UUID groupId;
    private final UUID senderId;
    private final byte[] content;
    private final Instant sentAt;

    public GroupMessage(UUID id, UUID groupId, UUID senderId, byte[] content, Instant sentAt) {
        this.id       = id;
        this.groupId  = groupId;
        this.senderId = senderId;
        this.content  = content;
        this.sentAt   = sentAt;
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
    public byte[] getContent() {
        return content;
    }
    public Instant getSentAt() {
        return sentAt;
    }
}
