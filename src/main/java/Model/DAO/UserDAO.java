package Model.DAO;

import Model.OBJ.Group;
import Model.OBJ.User;
import Utils.DatabaseConnection;

import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import java.util.List;

public class UserDAO {

    public UserDAO() {
    }

    /**
     * יוצר משתמש חדש בטבלת המשתמשים במסד הנתונים.
     * @param user אובייקט מסוג User עם נתוני המשתמש.
     * @return true אם ההוספה הצליחה, אחרת false.
     * @throws SQLException אם מתרחשת שגיאה במסד הנתונים.
     */
    public boolean createUser(User user) throws SQLException {
        String sql = """
                INSERT INTO Users
                (Id, Username, Email, PasswordHash, LastLogin)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getId().toString());
            stmt.setString(2, user.getUsername());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getPasswordHash());
            stmt.setTimestamp(5, Timestamp.from(user.getLastLogin()));
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * מחזיר את המשתמש לפי מזהה ייחודי (UUID).
     * @param userId מזהה המשתמש.
     * @return אובייקט User אם נמצא, אחרת null.
     * @throws SQLException אם מתרחשת שגיאה במסד הנתונים.
     */
    public User getUserById(UUID userId) throws SQLException {
        return getUser("SELECT * FROM Users WHERE Id = ?", userId.toString());
    }

    /**
     * מחפש משתמש לפי כתובת דוא"ל.
     * @param email כתובת האימייל של המשתמש.
     * @return אובייקט User אם נמצא, אחרת null.
     * @throws SQLException אם מתרחשת שגיאה במסד הנתונים.
     * @throws IOException אם מתרחשת שגיאה בקריאה.
     */
    public User getUserByEmail(String email) throws SQLException, IOException {
        return getUser("SELECT * FROM Users WHERE Email = ?", email);
    }

    // מתודת עזר למציאה
    private User getUser(String sql, Object param) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, param.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapUser(rs) : null;
            }
        }
    }

    public String getUsernameById(UUID userId) throws SQLException {
        String sql = "SELECT Username FROM Users WHERE Id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("Username");
                } else {
                    throw new SQLException("User not found");
                }
            }
        }
    }
    /**
     * מעדכן את זמן ההתחברות האחרון.
     * @param user אובייקט המשתמש לעדכון.
     * @return true אם הצליח, אחרת false.
     * @throws SQLException אם מתרחשת שגיאה במסד הנתונים.
     */
    public boolean updateUserLoginState(User user) throws SQLException {
        String sql = "UPDATE Users SET LastLogin = ? WHERE Id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.from(user.getLastLogin()));
            stmt.setObject(2, user.getId());
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * מעדכן את פרטי המשתמש במסד הנתונים. רק שדות שאינם null יעודכנו.
     * @param user האובייקט המכיל את הערכים החדשים.
     * @return true אם העדכון הצליח, אחרת false.
     */
    public boolean updateUser(User user) {
        StringBuilder query = new StringBuilder("UPDATE Users SET ");
        ArrayList<Object> params = new ArrayList<>();

        if (user.getUsername() != null) {
            query.append("Username = ?, ");
            params.add(user.getUsername());
        }
        if (user.getPasswordHash() != null) {
            query.append("PasswordHash = ?, ");
            params.add(user.getPasswordHash());
        }
        if (user.getEmail() != null) {
            query.append("Email = ?, ");
            params.add(user.getEmail());
        }

        if (user.getLastLogin() != null) {
            query.append("LastLogin = ?, ");
            params.add(user.getLastLogin());
        }

        query.append("FailedLogins = ?, ");
        params.add(user.getFailedLogins());

        if (user.getLockUntil() != null) {
            query.append("LockUntil = ?, ");
            params.add(user.getLockUntil());
        }

        // מחיקת פסיק מיותר בסוף השאילתה
        if (params.isEmpty()) {
            throw new IllegalArgumentException("No fields to update");
        }

        query.setLength(query.length() - 2);

        // הוספת תנאי WHERE כדי לעדכן רק משתמש מסוים
        query.append(" WHERE Id = ?");
        params.add(user.getId());

        // ביצוע השאילתה עם הפרמטרים שנאספו
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query.toString())) {

            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);

                if (param instanceof UUID) {
                    stmt.setString(i + 1, param.toString());
                } else if (param instanceof Instant) {
                    stmt.setTimestamp(i + 1, Timestamp.from((Instant) param));
                } else if (param instanceof Boolean) {
                    stmt.setBoolean(i + 1, (Boolean) param);
                } else {
                    stmt.setObject(i + 1, param);
                }
            }

            return stmt.executeUpdate() > 0;
        }catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Group> getAllGroups(UUID userId) throws SQLException{
        List<Group> result = new ArrayList<>();

        String sql = """
            SELECT g.GroupId, g.GroupName, g.CreatorId, g.CreatedAt
            FROM Groups g
            JOIN GroupMembers gm ON g.GroupId = gm.GroupId
            WHERE gm.UserId = ?
        """;

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)){

            stmt.setString(1, userId.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UUID groupId = UUID.fromString(rs.getString("GroupId"));
                    Instant createdAt = rs.getTimestamp("CreatedAt").toInstant();
                    UUID creatorId = UUID.fromString(rs.getString("CreatorId"));
                    String groupName = rs.getString("GroupName");

                    Group group = new Group(groupId, createdAt, creatorId, groupName);
                    result.add(group);
                }
            }
        }
        return  result;
    }

    public boolean deleteUser(UUID userId) throws SQLException {
        String sql = "DELETE FROM Users WHERE Id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, userId.toString());
            return stmt.executeUpdate() > 0;
        }
    }

    public List<User> getAllUsers() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM Users";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                users.add(mapUser(rs));
            }
        }

        return users;
    }

    public List<User> getUsersInGroup(UUID groupId) throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = """
            SELECT u.*
            FROM Users u
            JOIN GroupMembers gm ON u.Id = gm.UserId
            WHERE gm.GroupId = ?
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, groupId.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapUser(rs));
                }
            }
        }

        return users;
    }

    /**
     * ממפה תוצאה ממסד הנתונים לאובייקט מסוג User.
     * @param rs תוצאת השאילתה.
     * @return אובייקט מסוג User.
     * @throws SQLException אם מתרחשת שגיאה בגישה לנתונים.
     */
    public User mapUser(ResultSet rs) throws SQLException {
        UUID id = UUID.fromString(rs.getString("Id"));
        String username = rs.getString("Username");
        String email = rs.getString("Email");
        String passwordHash = rs.getString("PasswordHash");
        Instant lastLogin = rs.getTimestamp("LastLogin") != null ? rs.getTimestamp("LastLogin").toInstant() : null;
        int failedLogins = rs.getInt("FailedLogins");
        Instant lockUntil = rs.getTimestamp("LockUntil") != null ? rs.getTimestamp("LockUntil").toInstant() : null;

        User user = new User(id, username, email, passwordHash, lastLogin, failedLogins, lockUntil);
        user.setLastLogin(lastLogin);
        user.setFailedLogins(failedLogins);
        user.setLockUntil(lockUntil);
        return user;
    }

}
