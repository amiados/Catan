package Model;

import Security.PasswordHasher;
import Utils.ValidationResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

public class User {
    // מזהה ייחודי של המשתמש
    private final UUID id;
    // שם התצוגה שפנוי למשתמש וכל שאר המשתמשים רואים
    private String username;
    // סיסמה מוצפנת כ-hash לאימות
    private final String passwordHash;
    // כתובת אימייל לשחזור סיסמה ותקשורת
    private String email;
    // זמן הלוגין האחרון במערכת
    private Instant lastLogin;

    // ספירת ניסיונות כניסה כושלים ברצף
    private int failedLogins;
    // זמן עד מתי החשבון נעול (null אם לא נעול)
    private Instant lockUntil;

    public User(UUID id, String username, String email, String passwordHash) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
    }

    public User(String username, String email, String password) {
        this.id = UUID.randomUUID();
        this.username = username;
        this.passwordHash = PasswordHasher.hash(password);
        this.email = email;
        this.lastLogin = Instant.now();
        this.failedLogins = 0;
        this.lockUntil = null;
    }

    // --- ואלידציות ---

    /**
     * מאמת שם משתמש, אימייל וסיסמה לפי קריטריונים.
     *
     * @return ValidationResult עם סטטוס ושגיאות במידת הצורך.
     */
    public static ValidationResult validate(String username, String email, String password) {
        ArrayList<String> errors = new ArrayList<>();
        if (!emailValidator(email)) {
            errors.add("פורמט אימייל לא תקין. יש להשתמש בדוגמה: example@gmail.com");
        }
        if (!userNameValidator(username)) {
            errors.add("פורמט שם משתמש לא תקין. יש לפחות 6 תווים ו-3 אותיות לפחות.");
        }
        if (!passwordValidator(password)) {
            errors.add("פורמט סיסמה לא תקין. לפחות 8 תווים, אות גדולה, אות קטנה, ספרה ותו מיוחד.");
        }
        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * מאמת אימייל וסיסמה בלבד (לכניסה).
     */
    public static ValidationResult validate(String email, String password) {
        ArrayList<String> errors = new ArrayList<>();
        if (!emailValidator(email)) {
            errors.add("פורמט אימייל לא תקין. יש להשתמש בדוגמה: example@gmail.com");
        }
        if (!passwordValidator(password)) {
            errors.add("פורמט סיסמה לא תקין. לפחות 8 תווים, אות גדולה, אות קטנה, ספרה ותו מיוחד.");
        }
        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * בודק אם הסיסמה עומדת בתנאים:
     * לפחות 8 תווים, אות גדולה, אות קטנה, ספרה ותו מיוחד.
     */
    public static Boolean passwordValidator(String password) {
        String pattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[~!@#$%^&*.|/+-_=<>?]).{8,}$";
        return password != null && password.matches(pattern);
    }

    /**
     * בודק תקינות אימייל לפי ביטוי רגולרי.
     */
    public static Boolean emailValidator(String email) {
        String pattern = "^[a-zA-Z0-9._%&#!+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email != null && email.matches(pattern);
    }

    /**
     * בודק תקינות שם משתמש:
     * לפחות 6 תווים ו-3 אותיות לפחות.
     */
    public static Boolean userNameValidator(String userName) {
        String pattern = "^(?=(?:.*[a-zA-Z]){3,}).{6,}$";
        return userName != null && userName.matches(pattern);
    }

    // --- getters & setters ---

    public UUID getId() {
        return id;
    }
    public String getUsername() {
        return username;
    }
    public String getPasswordHash() {
        return passwordHash;
    }
    public String getEmail() {
        return email;
    }
    public Instant getLastLogin() {
        return lastLogin;
    }
    public int getFailedLogins() {
        return failedLogins;
    }
    public Instant getLockUntil() {
        return lockUntil;
    }

    public void setUsername(String username) {
        this.username = username;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public void setLastLogin(Instant lastLogin) {
        this.lastLogin = lastLogin;
    }
    public void setFailedLogins(int failedLogins) {
        this.failedLogins = failedLogins;
    }
    public void setLockUntil(Instant lockUntil) {
        this.lockUntil = lockUntil;
    }

    /**
     * בודק אם החשבון נעול כרגע
     * @return true אם נעול, false אחרת
     */
    public boolean isLocked() {
        return lockUntil != null && Instant.now().isBefore(lockUntil);
    }
}
