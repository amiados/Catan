package Model.dao;

import Model.obj.Message;
import Utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.UUID;

public class MessageDAO {

    public MessageDAO() {
    }

    /**
     * Inserts a new message into the Messages table.
     *
     * @param message the Messages object containing message data.
     * @return true if the message was inserted successfully, false otherwise.
     * @throws SQLException if a database access error occurs.
     */
    public boolean saveMessage(Message message) throws SQLException {
        String sql = """
        INSERT INTO Messages
            (MessageId, GameId, SenderId, Content, SentAt) VALUES 
            (?, ?, ?, ?, ?)
        """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, message.getId());
            stmt.setObject(2, message.getGameId());
            stmt.setObject(3, message.getSenderId());
            stmt.setBytes(4, message.getContent());
            stmt.setTimestamp(5, Timestamp.from(message.getSentAt()));
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Retrieves a paginated list of messages for a specific game.
     *
     * @param gameId the game's UUID.
     * @param limit the maximum number of messages to fetch.
     * @param offset the number of messages to skip (for pagination).
     * @return a list of Messages ordered by SentAt descending.
     * @throws SQLException if a database access error occurs.
     */
    public ArrayList<Message> getMessagesByGשצקId(UUID gameId, int limit, int offset) throws SQLException {
        String sql = """
        SELECT M.*
        FROM Messages M
        WHERE M.GameId = ?
        ORDER BY SentAt DESC
        OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
        """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, gameId);
            stmt.setInt(3, offset);
            stmt.setInt(4, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                ArrayList<Message> messages = new ArrayList<>();
                while (rs.next()) {
                    messages.add(mapResultSetToMessage(rs));
                }
                return messages;
            }
        }
    }

    /**
     * Maps a result set row to a Messages object.
     *
     * @param rs the result set from a query.
     * @return a populated Messages object.
     * @throws SQLException if data access fails.
     */
    private Message mapResultSetToMessage(ResultSet rs) throws SQLException {
        return new Message(
                UUID.fromString(rs.getString("Id")),
                UUID.fromString(rs.getString("GameId")),
                UUID.fromString(rs.getString("SenderId")),
                rs.getBytes("Content"),
                rs.getTimestamp("SentAt").toInstant()
        );
    }


}
