package Model.dao;

import Model.InviteStatus;
import Model.obj.Invite;
import Utils.DatabaseConnection;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

public class InviteDAO {

    public InviteDAO() {
    }

    /**
     * Inserts a new invite into the database.
     *
     * @param invite the Invite object to be created
     * @return true if the invite was successfully created, false otherwise (e.g., constraint violation)
     * @throws SQLException if a database access error occurs
     */
    public boolean createInvite(Invite invite) throws SQLException {
        String sql = """
    INSERT INTO Invites
        (InviteId, GameId, senderId, receiverId, status, sentAt)
        VALUES (?, ?, ?, ?, ?, ?)
        """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, invite.getId().toString());
            stmt.setString(2, invite.getGameId().toString());
            stmt.setString(3, invite.getSenderId().toString());
            stmt.setString(4, invite.getReceiverId().toString());
            stmt.setString(6, invite.getStatus().name());
            stmt.setTimestamp(5, Timestamp.from(invite.getSentAt()));
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean isInviteExist(UUID gameId, UUID receiverId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Invites WHERE GameId = ? AND receiverId = ? AND Status = 'PENDING'";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, gameId);
            stmt.setObject(2, receiverId);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    public Invite getInvite(UUID gameId, UUID receiverId) throws SQLException {
        String sql = "SELECT * FROM Invites WHERE GameId = ? AND receiverId = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, gameId);
            stmt.setObject(2, receiverId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToInvite(rs);
            }
            return null;
        }
    }

    public Invite getInviteById(UUID receiverId) throws SQLException {
        String sql = "SELECT * FROM Invites WHERE receiverId = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, receiverId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToInvite(rs);
            }
            return null;
        }
    }

    /**
     * מעדכן את כל ההזמנות שסטטוסן PENDING ונשלחו לפני cutoff ל־EXPIRED.
     * @param cutoff נקודת זמן–כל הזמנה ישנה ממנה תוקם
     * @return מספר ההזמנות שיומרו
     */
    public int expirePendingInvites(Instant cutoff) throws SQLException {
        String sql = """
        UPDATE Invites
        SET Status = 'EXPIRED'
        WHERE Status = 'PENDING'
          AND SentAt < ?
    """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.from(cutoff));
            return ps.executeUpdate();
        }
    }

    /**
     * Retrieves all invites for a specific user (only relevant invites).
     * Can be used for analytics or notifications.
     *
     * @param userId the invited user ID
     * @return a list of Invite objects
     * @throws SQLException if a database access error occurs
     */
    public ArrayList<Invite> getUserInvites(UUID userId) throws SQLException {
        String sql = "SELECT * FROM Invites WHERE receiverId = ? AND Status = 'PENDING'";
        ArrayList<Invite> invites = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    invites.add(mapResultSetToInvite(rs));
                }
            }
        }
        return invites;
    }

    private Invite mapResultSetToInvite(ResultSet rs) throws SQLException {
        return new Invite(
                UUID.fromString(rs.getString("InviteId")),
                UUID.fromString(rs.getString("GameId")),
                UUID.fromString(rs.getString("senderId")),
                UUID.fromString(rs.getString("receiverId")),
                InviteStatus.valueOf(rs.getString("Status")),
                rs.getTimestamp("SentAt").toInstant()

        );
    }

}
