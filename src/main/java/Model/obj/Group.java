package Model.obj;

import java.time.Instant;
import java.util.UUID;

public class Group {
    private final UUID groupId;
    private String groupName;
    private UUID creatorId;
    private Instant createdAt;

    public Group(UUID groupId, Instant createdAt, UUID creatorId, String groupName) {
        this.groupId = groupId;
        this.createdAt = createdAt;
        this.creatorId = creatorId;
        this.groupName = groupName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
    public UUID getCreatorId() {
        return creatorId;
    }
    public UUID getGroupId() {
        return groupId;
    }
    public String getGroupName() {
        return groupName;
    }
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    public void setCreatorId(UUID creatorId) {
        this.creatorId = creatorId;
    }
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}
