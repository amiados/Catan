package Model.DAO;

import Model.OBJ.Game;
import Utils.DatabaseConnection;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GameDAO {

    public GameDAO() {
    }

    public boolean createGame(Game game) throws SQLException {
        String sql = """
                INSERT INTO Games 
                    (GameId, GroupId, CreatedAt, StartedAt, EndedAt, HostId)
                    VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, game.getGameId());
            ps.setObject(2, game.getGroupId());
            ps.setTimestamp(3, Timestamp.from(game.getCreatedAt()));
            ps.setTimestamp(4, game.getStartedAt() == null ? null : Timestamp.from(game.getStartedAt()));
            ps.setTimestamp(5, game.getEndedAt() == null ? null : Timestamp.from(game.getEndedAt()));
            ps.setObject(6, game.getHostId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean startGame(UUID gameId, Instant startedAt) throws SQLException {
        String sql = "UPDATE Games SET StartedAt = ? WHERE GameId = ? AND StartedAt IS NULL";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.from(startedAt));
            stmt.setObject(2, gameId);

            int updatedRows = stmt.executeUpdate();
            return updatedRows > 0;
        }
    }

    public Game getGameById(UUID gameId) throws SQLException{
        String sql = "SELECT * FROM Games WHERE GameId = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, gameId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    parseGame(rs);
                }
                return null;
            }
        }
    }

    public List<Game> getGamesByUserId(UUID playerId) throws SQLException {
        String sql = "SELECT * FROM Games WHERE HostId = ?";
        List<Game> games = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    games.add(parseGame(rs));
                }
            }
        }
        return games;
    }

    public List<Game> getGamesByGroupId(UUID groupId) throws SQLException {
        String sql = "SELECT * FROM Games WHERE GroupId = ?";
        List<Game> games = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    games.add(parseGame(rs));
                }
            }
        }
        return games;
    }

    public int countGames() throws SQLException {
        String sql = "SELECT COUNT(*) FROM Games";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private Game parseGame(ResultSet rs) throws SQLException {
        return new Game(
                UUID.fromString(rs.getString("GameId")),
                UUID.fromString(rs.getString("GroupId")),
                rs.getTimestamp("CreatedAt").toInstant(),
                rs.getTimestamp("StartedAt") == null ? null : rs.getTimestamp("StartedAt").toInstant(),
                rs.getTimestamp("EndedAt") == null ? null : rs.getTimestamp("EndedAt").toInstant(),
                UUID.fromString(rs.getString("HostId"))
        );
    }
}
