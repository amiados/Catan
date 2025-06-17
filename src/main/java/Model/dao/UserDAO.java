package Model.dao;

import Model.obj.Group;
import Model.obj.User;
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
        String sql = """
            SELECT g.GroupId, g.GroupName, u.Username
            FROM Groups g
            JOIN Users u ON g.CreatorId = u.Id
            WHERE g.GroupId IN (
                SELECT GroupId
                FROM GroupMembers
                WHERE UserId = ?
            )
        """;

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)){

            stmt.setString(1, userId.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next()
            }
        }
    }

    /**
     * ממפה תוצאה ממסד הנתונים לאובייקט מסוג User.
     * @param rs תוצאת השאילתה.
     * @return אובייקט מסוג User.
     * @throws SQLException אם מתרחשת שגיאה בגישה לנתונים.
     */
    public User mapUser(ResultSet rs) throws SQLException {
        String idString = rs.getString("Id");
        UUID id = idString != null ? UUID.fromString(idString) : null;
        String username = rs.getString("Username");
        String email = rs.getString("Email");
        String passwordHash = rs.getString("PasswordHash");
        Timestamp lastLoginTs = rs.getTimestamp("LastLogin");
        Instant lastLogin = lastLoginTs != null ? lastLoginTs.toInstant() : null;
        int failedLogins = rs.getInt("failed_logins");
        Timestamp lockTs = rs.getTimestamp("lock_until");
        Instant lockUntil = lockTs != null ? lockTs.toInstant() : null;

        User user = new User(id, username, email, passwordHash);
        user.setLastLogin(lastLogin);
        user.setFailedLogins(failedLogins);
        user.setLockUntil(lockUntil);
        return user;
    }

}
