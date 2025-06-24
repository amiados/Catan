package Model.DAO;

import Model.OBJ.Player;
import Model.OBJ.User;
import Utils.DatabaseConnection;
import catan.PieceColor;

import java.sql.*;
import java.time.Instant;
import java.util.*;

public class PlayerDAO {

    public boolean addPlayerToGame(Player player, UUID gameId) throws SQLException {
        String sql = """
            INSERT INTO Players
            (PlayerId, UserId, GameId, Color, BonusVictoryPoints, ArmySize, LongestRoadLength, HasLargestArmy, HasLongestRoad, IsTurn)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, player.getPlayerId());
            ps.setObject(2, player.getUserId());
            ps.setObject(3, gameId);
            ps.setString(4, player.getPieceColor().name());
            ps.setInt(5, player.getBonusVictoryPoints());
            ps.setInt(6, player.getArmySize());
            ps.setInt(7, player.getRoadLength());
            ps.setBoolean(8, player.hasLargestArmy());
            ps.setBoolean(9, player.hasLongestRoad());
            ps.setBoolean(10, player.isTurn());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updatePlayerColor(UUID playerId, PieceColor newColor) throws SQLException {
        String sql = "UPDATE Players SET Color = ? WHERE PlayerId = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newColor.name());
            ps.setObject(2, playerId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updatePlayerTurn(UUID playerId, boolean isTurn) throws SQLException {
        String sql = "UPDATE Players SET IsTurn = ? WHERE PlayerId = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, isTurn);
            ps.setObject(2, playerId);
            return ps.executeUpdate() > 0;
        }
    }

    public Player getPlayerById(UUID playerId) throws SQLException {
        String sql = """
            SELECT p.*, u.Username, u.Email, u.PasswordHash, u.LastLogin, u.FailedLogins, u.LockUntil
            FROM Players p
            JOIN Users u ON p.UserId = u.Id
            WHERE p.PlayerId = ?
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapPlayer(rs) : null;
            }
        }
    }

    public List<Player> getPlayersByGame(UUID gameId) throws SQLException {
        String sql = """
            SELECT p.*, u.Username, u.Email, u.PasswordHash, u.LastLogin, u.FailedLogins, u.LockUntil
            FROM Players p
            JOIN Users u ON p.UserId = u.Id
            WHERE p.GameId = ?
        """;

        List<Player> players = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, gameId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    players.add(mapPlayer(rs));
                }
            }
        }

        return players;
    }

    public boolean updateVictoryStatus(Player player) throws SQLException {
        String sql = """
            UPDATE Players
            SET BonusVictoryPoints = ?, ArmySize = ?, LongestRoadLength = ?, 
                HasLargestArmy = ?, HasLongestRoad = ?
            WHERE PlayerId = ?
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, player.getBonusVictoryPoints());
            ps.setInt(2, player.getArmySize());
            ps.setInt(3, player.getRoadLength());
            ps.setBoolean(4, player.hasLargestArmy());
            ps.setBoolean(5, player.hasLongestRoad());
            ps.setObject(6, player.getPlayerId());

            return ps.executeUpdate() > 0;
        }
    }

    public boolean removePlayer(UUID playerId) throws SQLException {
        String sql = "DELETE FROM Players WHERE PlayerId = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, playerId);
            return ps.executeUpdate() > 0;
        }
    }

    private Player mapPlayer(ResultSet rs) throws SQLException {
        UUID playerId = UUID.fromString(rs.getString("PlayerId"));
        UUID userId = UUID.fromString(rs.getString("UserId"));
        String username = rs.getString("Username");
        String email = rs.getString("Email");
        String passwordHash = rs.getString("PasswordHash");
        Instant lastLogin = rs.getTimestamp("LastLogin") != null ? rs.getTimestamp("LastLogin").toInstant() : null;
        int failedLogins = rs.getInt("FailedLogins");
        Instant lockUntil = rs.getTimestamp("LockUntil") != null ? rs.getTimestamp("LockUntil").toInstant() : null;
        PieceColor color = PieceColor.valueOf(rs.getString("Color"));
        boolean isTurn = rs.getBoolean("IsTurn");

        User user = new User(userId, username, email, passwordHash, lastLogin, failedLogins, lockUntil);
        Player player = new Player(playerId, user, color);
        player.setTurn(isTurn);
        return player;
    }
}
