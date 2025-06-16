package Model;

import Utils.DatabaseConnection;
import catan.PieceColor;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerDAO {

    public PlayerDAO() {
    }

    public boolean addPlayerToGame(Player player, UUID gameId) throws SQLException {
        String sql = """
            INSERT INTO Players
            (PlayerId, UserId, GameId, Color, BonusVictoryPoints, ArmySize, LongestRoadLength, HasLargestArmy, HasLongestRoad)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, player.getId());
            ps.setObject(2, player.getId());
            ps.setObject(3, gameId);
            ps.setString(4, player.getPieceColor().name());
            ps.setInt(5, player.getBonusVictoryPoints());
            ps.setInt(6, player.getArmySize());
            ps.setInt(7, player.getRoadLength());
            ps.setBoolean(8, player.hasLargestArmy());
            ps.setBoolean(9, player.hasLongestRoad());
            return ps.executeUpdate() > 0;
        }
    }

    public List<Player> getPlayersByGame(UUID gameId) throws SQLException {
        String sql = """
            SELECT p.*, u.Username, u.Email, u.PasswordHash
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
                    UUID playerId = UUID.fromString(rs.getString("PlayerId"));
                    UUID userId = UUID.fromString(rs.getString("UserId"));
                    String username = rs.getString("Username");
                    String email = rs.getString("Email");
                    String passwordHash = rs.getString("PasswordHash");
                    PieceColor color = PieceColor.valueOf(rs.getString("Color"));

                    User user = new User(userId, username, email, passwordHash);
                    Player player = new Player(playerId, user, color);
                    players.add(player);
                }
            }
        }
        return players;
    }

    public Player getPlayerById(UUID playerId) throws SQLException {
        String sql = """
                SELECT p.*, u.Username, u.Email, u.PasswordHash
                FROM Players p
                JOIN Users u on p.UserId = u.UserId
                WHERE p.PlayerId = ?
                """;
        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, playerId);
            try (ResultSet rs = ps.executeQuery()){
                if (rs.next()) {
                    UUID userId = UUID.fromString(rs.getString("UserId"));
                    String username = rs.getString("Username");
                    String email = rs.getString("Email");
                    String passwordHash = rs.getString("PasswordHash");
                    PieceColor color = PieceColor.valueOf(rs.getString("Color"));

                    User user = new User(userId, username, email, passwordHash);
                    return new Player(playerId, user, color);
                }
                return null;
            }
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
}
