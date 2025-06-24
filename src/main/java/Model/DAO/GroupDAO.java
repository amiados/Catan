package Model.DAO;

import Model.OBJ.Group;
import Utils.DatabaseConnection;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GroupDAO {

    public GroupDAO() {
    }

    public boolean createGroup(Group group) throws SQLException {
        String sql = """
            INSERT INTO Groups (GroupId, GroupName, CreatorId, CreatedAt)
            VALUES (?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, group.getGroupId());
            stmt.setString(2, group.getGroupName());
            stmt.setObject(3, group.getCreatorId());
            stmt.setTimestamp(4, Timestamp.from(group.getCreatedAt()));
            return stmt.executeUpdate() > 0;
        }
    }

    public List<Group> getGroupsForUser(UUID userId) throws SQLException {
        String sql = """
            SELECT g.GroupId, g.GroupName, g.CreatorId, g.CreatedAt
            FROM Groups g
            JOIN GroupMembers gm ON g.GroupId = gm.GroupId
            WHERE gm.UserId = ?
        """;

        List<Group> groups = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UUID groupId = UUID.fromString(rs.getString("GroupId"));
                    String name = rs.getString("GroupName");
                    UUID creatorId = UUID.fromString(rs.getString("CreatorId"));
                    Instant createdAt = rs.getTimestamp("CreatedAt").toInstant();

                    groups.add(new Group(groupId, createdAt, creatorId, name));
                }
            }
        }
        return groups;
    }

    public Group getGroupById(UUID groupId) throws SQLException {
        String sql = "SELECT * FROM Groups WHERE GroupId = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, groupId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String name = rs.getString("GroupName");
                    UUID creatorId = UUID.fromString(rs.getString("CreatorId"));
                    Instant createdAt = rs.getTimestamp("CreatedAt").toInstant();
                    return new Group(groupId, createdAt, creatorId, name);
                }
                return null;
            }
        }
    }

}

