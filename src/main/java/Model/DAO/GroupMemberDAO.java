package Model.DAO;

import Model.MemberRole;
import Utils.DatabaseConnection;

import java.sql.*;
import java.time.Instant;
import java.util.UUID;

public class GroupMemberDAO {

    public GroupMemberDAO() {
    }

    public boolean addMemberToGroup(UUID groupId, UUID userId, MemberRole role) throws SQLException {
        String sql = """
            INSERT INTO GroupMembers (GroupId, UserId, Role, JoinedAt)
            VALUES (?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, groupId);
            stmt.setObject(2, userId);
            stmt.setString(3, role.name());
            stmt.setTimestamp(4, Timestamp.from(Instant.now()));
            return stmt.executeUpdate() > 0;
        }
    }

    public MemberRole getUserRole(UUID groupId, UUID userId) throws SQLException {
        String sql = "SELECT Role FROM GroupMembers WHERE GroupId = ? AND UserId = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, groupId);
            stmt.setObject(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String roleStr = rs.getString("Role");
                    try {
                        return MemberRole.valueOf(roleStr.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        // במידה וערך לא חוקי נשמר בטבלה (למקרה חריג)
                        throw new SQLException("Unknown role type in database: " + roleStr, e);
                    }
                } else {
                    return null; // No such member found
                }
            }
        }
    }

    public boolean isUserInGroup(UUID groupId, UUID userId) throws SQLException {
        String sql = "SELECT 1 FROM GroupMembers WHERE GroupId = ? AND UserId = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, groupId);
            stmt.setObject(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean removeMember(UUID groupId, UUID userId) throws SQLException {
        String sql = "DELETE FROM GroupMembers WHERE GroupId = ? AND UserId = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, groupId);
            stmt.setObject(2, userId);
            return stmt.executeUpdate() > 0;
        }
    }
}
