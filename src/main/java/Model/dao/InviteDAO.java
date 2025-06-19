package Model.dao;

import Model.InviteStatus;
import Model.obj.Invite;
import Utils.DatabaseConnection;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InviteDAO {

    public boolean createInvite(Invite invite) throws SQLException {
        String sql = """
            INSERT INTO GameInvites
            (InviteId, GroupId, FromUserId, ToUserId, Status, CreatedAt)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, invite.getId().toString());
            stmt.setString(2, invite.getGroupId().toString());
            stmt.setString(3, invite.getSenderId().toString());
            stmt.setString(4, invite.getReceiverId().toString());
            stmt.setString(5, invite.getStatus().name());
            stmt.setTimestamp(6, Timestamp.from(invite.getSentAt()));

            return stmt.executeUpdate() > 0;
        }
    }

    public Invite getInvite(UUID groupId, UUID receiverId) throws SQLException {
        String sql = """
            SELECT * FROM GameInvites 
            WHERE GroupId = ? AND ToUserId = ? AND Status = 'PENDING'
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, groupId.toString());
            stmt.setString(2, receiverId.toString());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapResultSetToInvite(rs);
        }
        return null;
    }

    public Invite getInviteById(UUID inviteId) throws SQLException {
        String sql = "SELECT * FROM GroupInvites WHERE InviteId = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, inviteId.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapResultSetToInvite(rs);
            }
        }
        return null;
    }

    public List<Invite> getSentInvitesByUser(UUID userId) throws SQLException {
        String sql = """
        SELECT * FROM GameInvites
        WHERE FromUserId = ?
    """;
        ArrayList<Invite> invites = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, userId.toString());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                invites.add(mapResultSetToInvite(rs));
            }
        }
        return invites;
    }

    public List<Invite> getUserPendingInvites(UUID userId) throws SQLException {
        String sql = """
            SELECT * FROM GameInvites
            WHERE ToUserId = ? AND Status = 'PENDING'
        """;
        ArrayList<Invite> invites = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, userId.toString());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                invites.add(mapResultSetToInvite(rs));
            }
        }
        return invites;
    }

    public boolean updateInviteStatus(UUID inviteId, InviteStatus newStatus) throws SQLException {
        String sql = """
            UPDATE GameInvites 
            SET Status = ? 
            WHERE InviteId = ?
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newStatus.name());
            stmt.setString(2, inviteId.toString());
            return stmt.executeUpdate() > 0;
        }
    }

    public int expireOldPendingInvites(Instant cutoff) throws SQLException {
        String sql = """
            UPDATE GameInvites
            SET Status = 'EXPIRED'
            WHERE Status = 'PENDING' AND CreatedAt < ?
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.from(cutoff));
            return stmt.executeUpdate();
        }
    }

    private Invite mapResultSetToInvite(ResultSet rs) throws SQLException {
        return new Invite(
                UUID.fromString(rs.getString("InviteId")),
                UUID.fromString(rs.getString("GroupId")),
                UUID.fromString(rs.getString("FromUserId")),
                UUID.fromString(rs.getString("ToUserId")),
                InviteStatus.valueOf(rs.getString("Status")),
                rs.getTimestamp("CreatedAt").toInstant()
        );
    }

    public boolean deleteInvite(UUID inviteId) throws SQLException {
        String sql = "DELETE FROM GameInvites WHERE InviteId = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, inviteId.toString());
            return stmt.executeUpdate() > 0;
        }
    }
}
