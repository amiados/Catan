package Model.DAO;

import Model.OBJ.GameMessage;
import Model.OBJ.GroupMessage;
import Utils.DatabaseConnection;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

public class MessageDAO {

    public MessageDAO() {}

    // ================================
    // Game Chat
    // ================================

    /**
     * Inserts a new game‐chat message into the GameMessages table.
     */
    public boolean saveGameMessage(GameMessage message) throws SQLException {
        String sql = """
            INSERT INTO game_chat_messages
                (id, game_id, sender_id, content, timestamp)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, message.getId());
            ps.setObject(2, message.getGameId());
            ps.setObject(3, message.getSenderId());
            ps.setBytes(4, message.getContent());
            ps.setTimestamp(5, Timestamp.from(message.getSentAt()));
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Fetches a page of game‐chat messages for a given game.
     */
    public ArrayList<GameMessage> getGameMessages(UUID gameId, int limit, int offset) throws SQLException {
        String sql = """
            SELECT * FROM game_chat_messages
             WHERE game_id = ?
             ORDER BY timestamp DESC, id
             OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, gameId);
            ps.setInt(2, offset);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                var list = new ArrayList<GameMessage>();
                while (rs.next()) {
                    list.add(mapRowToGameMessage(rs));
                }
                return list;
            }
        }
    }

    private GameMessage mapRowToGameMessage(ResultSet rs) throws SQLException {
        return new GameMessage(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("game_id")),
                UUID.fromString(rs.getString("sender_id")),
                rs.getBytes("content"),
                rs.getTimestamp("timestamp").toInstant()
        );
    }

    // ================================
    // Group Chat
    // ================================

    /**
     * Inserts a new group‐chat message into the GroupMessages table.
     */
    public boolean saveGroupMessage(GroupMessage message) throws SQLException {
        String sql = """
            INSERT INTO group_chat_messages
                (id, group_id, sender_id, content, timestamp)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, message.getId());
            ps.setObject(2, message.getGroupId());
            ps.setObject(3, message.getSenderId());
            ps.setBytes(4, message.getContent());
            ps.setTimestamp(5, Timestamp.from(message.getSentAt()));
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Fetches a page of group‐chat messages for a given group.
     */
    public ArrayList<GroupMessage> getGroupMessages(UUID groupId, int limit, int offset) throws SQLException {
        String sql = """
            SELECT * FROM group_chat_messages
             WHERE group_id = ?
             ORDER BY timestamp DESC, id
             OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, groupId);
            ps.setInt(2, offset);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                var list = new ArrayList<GroupMessage>();
                while (rs.next()) {
                    list.add(mapRowToGroupMessage(rs));
                }
                return list;
            }
        }
    }

    private GroupMessage mapRowToGroupMessage(ResultSet rs) throws SQLException {
        return new GroupMessage(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("group_id")),
                UUID.fromString(rs.getString("sender_id")),
                rs.getBytes("content"),
                rs.getTimestamp("timestamp").toInstant()
        );
    }
}
