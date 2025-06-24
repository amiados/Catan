package Model.OBJ;

import Model.MemberRole;

import java.time.Instant;
import java.util.UUID;

public class GroupMember {

    private final UUID groupId;
    private final UUID userId;
    private MemberRole role;
    private Instant joinedAt;

    public GroupMember(UUID groupId, UUID userId, MemberRole role, Instant joinedAt) {
        this.groupId = groupId;
        this.userId = userId;
        this.role = role;
        this.joinedAt = joinedAt;
    }

    public UUID getGroupId() {
        return groupId;
    }
    public Instant getJoinedAt() {
        return joinedAt;
    }
    public MemberRole getRole() {
        return role;
    }
    public UUID getUserId() {
        return userId;
    }
    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }
    public void setRole(MemberRole role) {
        this.role = role;
    }
}
