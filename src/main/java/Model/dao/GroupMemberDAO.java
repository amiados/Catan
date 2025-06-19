package Model.dao;

import Utils.DatabaseConnection;

import java.sql.*;
import java.time.Instant;
import java.util.UUID;

public class GroupMemberDAO {

    public GroupMemberDAO() {
    }

    public boolean addMemberToGroup(UUID groupId, UUID userId, String role) throws SQLException {
        String sql = """
            INSERT INTO GroupMembers (GroupId, UserId, Role, JoinedAt)
            VALUES (?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, groupId);
            stmt.setObject(2, userId);
            stmt.setString(3, role);
            stmt.setTimestamp(4, Timestamp.from(Instant.now()));
            return stmt.executeUpdate() > 0;
        }
    }

    public String getUserRole(UUID groupId, UUID userId) throws SQLException {
        String sql = "SELECT Role FROM GroupMembers WHERE GroupId = ? AND UserId = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, groupId);
            stmt.setObject(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString("Role") : null;
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
