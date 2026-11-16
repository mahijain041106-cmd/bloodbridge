package backend;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Module 1 logic: register, login, logout, roles, activate/deactivate,
 * change password, reset password.
 * Validation problems throw IllegalArgumentException / IllegalStateException
 * with a message that is safe to show to the user.
 * All SQL uses PreparedStatement (no string concatenation of user input).
 * There is no session table: "logged in" is just the currentUser field.
 */
public class AuthManager {
    private User currentUser = null;

    public User getCurrentUser() { return currentUser; }
    public boolean isLoggedIn()  { return currentUser != null; }

    // ---------- Registration ----------

    /** Public self-registration. ADMIN cannot be chosen here. */
    public void register(String fullName, String username, String email, String password, String roleName) {
        if (!"REQUESTER".equals(roleName) && !"INVENTORY_OPERATOR".equals(roleName)) {
            throw new IllegalArgumentException("You can only register as REQUESTER or INVENTORY_OPERATOR.");
        }
        createUser(fullName, username, email, password, roleName);
    }

    /** Used only by the --seed-admin option in Main. */
    public void createAdmin(String fullName, String username, String email, String password) {
        createUser(fullName, username, email, password, "ADMIN");
    }

    private void createUser(String fullName, String username, String email, String password, String roleName) {
        fullName = fullName == null ? "" : fullName.trim();
        username = username == null ? "" : username.trim();
        email = email == null ? "" : email.trim().toLowerCase();

        if (fullName.isEmpty() || username.isEmpty() || email.isEmpty())
            throw new IllegalArgumentException("Full name, username and email are required.");
        if (!username.matches("[A-Za-z0-9_]{3,30}"))
            throw new IllegalArgumentException("Username must be 3-30 letters, digits or underscores.");
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
            throw new IllegalArgumentException("Email address is not valid.");
        if (!PasswordManager.isStrongEnough(password))
            throw new IllegalArgumentException("Password needs 8+ characters with a letter and a digit.");

        String hash = PasswordManager.hashPassword(password);
        String insertUser = "INSERT INTO users (full_name, username, email, password_hash) VALUES (?, ?, ?, ?)";
        String insertRole = "INSERT INTO user_roles (user_id, role_id) SELECT ?, role_id FROM roles WHERE role_name = ?";

        try (Connection con = jdbcconnection.getConnection()) {
            con.setAutoCommit(false); // user row and role row must succeed together
            try {
                int newId;
                try (PreparedStatement ps = con.prepareStatement(insertUser, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, fullName);
                    ps.setString(2, username);
                    ps.setString(3, email);
                    ps.setString(4, hash);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        newId = keys.getInt(1);
                    }
                }
                try (PreparedStatement ps = con.prepareStatement(insertRole)) {
                    ps.setInt(1, newId);
                    ps.setString(2, roleName);
                    if (ps.executeUpdate() == 0)
                        throw new IllegalStateException("Role missing. Run sql/02_module1_test_data.sql first.");
                }
                AuditLogger.log(con, newId, "USER_REGISTERED", "USER", newId, null,
                        AuditLogger.json("username", username, "email", email, "role", roleName), "New account registered");
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                if (e.getErrorCode() == 1062) // duplicate key
                    throw new IllegalArgumentException("That username or email is already registered.");
                throw e;
            } catch (RuntimeException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    // ---------- Login / logout ----------

    public User login(String username, String password) {
        if (username == null || password == null || username.trim().isEmpty() || password.isEmpty())
            throw new IllegalArgumentException("Enter username and password.");

        String sql = "SELECT u.user_id, u.full_name, u.username, u.email, u.password_hash, u.is_active, r.role_name "
                   + "FROM users u JOIN user_roles ur ON u.user_id = ur.user_id "
                   + "JOIN roles r ON ur.role_id = r.role_id WHERE u.username = ?";
        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                // Same message for "no such user" and "wrong password" so attackers learn nothing.
                boolean found = rs.next();
                if (!found || !PasswordManager.verifyPassword(password, rs.getString("password_hash"))) {
                    Integer actor = null;
                    if (found) actor = rs.getInt("user_id");
                    AuditLogger.log(con, actor, "LOGIN_FAILED", "USER", actor, null, null,
                            "Failed login for username '" + username.trim() + "'");
                    throw new IllegalArgumentException("Invalid username or password.");
                }
                if (!rs.getBoolean("is_active"))
                    throw new IllegalStateException("This account is deactivated. Contact an admin.");

                User user = new User(rs.getInt("user_id"), rs.getString("full_name"), rs.getString("username"),
                                     rs.getString("email"), true, rs.getString("role_name"));
                try (PreparedStatement up = con.prepareStatement("UPDATE users SET last_login_at = NOW() WHERE user_id = ?")) {
                    up.setInt(1, user.getUserId());
                    up.executeUpdate();
                }
                AuditLogger.log(con, user.getUserId(), "USER_LOGIN", "USER", user.getUserId(), null, null,
                        "Logged in as " + user.getRole());
                currentUser = user;
                return user;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    public void logout() {
        if (currentUser != null) {
            try {
                AuditLogger.log(currentUser.getUserId(), "USER_LOGOUT", "USER", currentUser.getUserId(), null, null, "Logged out");
            } catch (RuntimeException e) {
                // logging out must still work even if the audit write fails
                System.err.println("Warning: could not write audit record: " + e.getMessage());
            }
        }
        currentUser = null; // no session table: just clear the logged-in state
    }

    /** Role lookup by user id (login already loads the role; this is for other modules). */
    public String getRole(int userId) {
        String sql = "SELECT r.role_name FROM user_roles ur JOIN roles r ON ur.role_id = r.role_id WHERE ur.user_id = ?";
        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    public boolean hasRole(String roleName) {
        return currentUser != null && currentUser.getRole().equals(roleName);
    }

    // ---------- Activate / deactivate (ADMIN only) ----------

    public void deactivateUser(String targetUsername) {
        requireAdmin();
        if (currentUser.getUsername().equalsIgnoreCase(targetUsername.trim()))
            throw new IllegalArgumentException("You cannot deactivate your own account.");
        setActive(targetUsername, false);
    }

    public void activateUser(String targetUsername) {
        requireAdmin();
        setActive(targetUsername, true);
    }

    private void requireAdmin() {
        if (!hasRole("ADMIN")) throw new IllegalStateException("Only an ADMIN can do this.");
    }

    private void setActive(String targetUsername, boolean active) {
        try (Connection con = jdbcconnection.getConnection()) {
            con.setAutoCommit(false); // the change and its audit record succeed or fail together
            try {
                int targetId;
                boolean wasActive;
                try (PreparedStatement ps = con.prepareStatement("SELECT user_id, is_active FROM users WHERE username = ? FOR UPDATE")) {
                    ps.setString(1, targetUsername.trim());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("No user named '" + targetUsername + "'.");
                        targetId = rs.getInt(1);
                        wasActive = rs.getBoolean(2);
                    }
                }
                try (PreparedStatement ps = con.prepareStatement("UPDATE users SET is_active = ? WHERE user_id = ?")) {
                    ps.setBoolean(1, active);
                    ps.setInt(2, targetId);
                    ps.executeUpdate();
                }
                AuditLogger.log(con, currentUser.getUserId(), active ? "USER_ACTIVATED" : "USER_DEACTIVATED", "USER", targetId,
                        AuditLogger.json("is_active", String.valueOf(wasActive)),
                        AuditLogger.json("is_active", String.valueOf(active)),
                        (active ? "Activated" : "Deactivated") + " account '" + targetUsername.trim() + "'");
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } catch (RuntimeException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    // ---------- Password change ----------

    public void changePassword(String oldPassword, String newPassword) {
        if (currentUser == null) throw new IllegalStateException("Log in first.");
        if (!PasswordManager.isStrongEnough(newPassword))
            throw new IllegalArgumentException("New password needs 8+ characters with a letter and a digit.");
        try (Connection con = jdbcconnection.getConnection()) {
            con.setAutoCommit(false); // password update and audit record together
            String stored;
            try (PreparedStatement ps = con.prepareStatement("SELECT password_hash FROM users WHERE user_id = ?")) {
                ps.setInt(1, currentUser.getUserId());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    stored = rs.getString(1);
                }
            }
            if (!PasswordManager.verifyPassword(oldPassword, stored))
                throw new IllegalArgumentException("Current password is wrong.");
            try (PreparedStatement ps = con.prepareStatement("UPDATE users SET password_hash = ? WHERE user_id = ?")) {
                ps.setString(1, PasswordManager.hashPassword(newPassword));
                ps.setInt(2, currentUser.getUserId());
                ps.executeUpdate();
            }
            AuditLogger.log(con, currentUser.getUserId(), "PASSWORD_CHANGED", "USER", currentUser.getUserId(), null, null,
                    "Password changed");
            con.commit();
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    // ---------- Password reset ----------

    /**
     * Creates a reset token valid for 15 minutes. There is no email service, so the token is
     * returned and Main prints it. Returns null if the email is unknown (caller shows a neutral message).
     */
    public String requestPasswordReset(String email) {
        if (email == null || email.trim().isEmpty()) throw new IllegalArgumentException("Enter your email.");
        try (Connection con = jdbcconnection.getConnection()) {
            int userId;
            try (PreparedStatement ps = con.prepareStatement("SELECT user_id FROM users WHERE email = ? AND is_active = TRUE")) {
                ps.setString(1, email.trim().toLowerCase());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return null;
                    userId = rs.getInt(1);
                }
            }
            con.setAutoCommit(false);
            try {
                // Old unused tokens stop working when a new one is issued.
                try (PreparedStatement ps = con.prepareStatement("UPDATE password_reset_tokens SET is_used = TRUE WHERE user_id = ? AND is_used = FALSE")) {
                    ps.setInt(1, userId);
                    ps.executeUpdate();
                }
                String token = newToken();
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO password_reset_tokens (user_id, reset_token, expiry_time) VALUES (?, ?, DATE_ADD(NOW(), INTERVAL 15 MINUTE))")) {
                    ps.setInt(1, userId);
                    ps.setString(2, token);
                    ps.executeUpdate();
                }
                AuditLogger.log(con, userId, "PASSWORD_RESET_REQUESTED", "USER", userId, null, null,
                        "Reset token issued (valid 15 minutes)");
                con.commit();
                return token;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    /** Uses a token once: must exist, be unused and not expired. */
    public void resetPassword(String token, String newPassword) {
        if (token == null || token.trim().isEmpty()) throw new IllegalArgumentException("Enter the reset token.");
        if (!PasswordManager.isStrongEnough(newPassword))
            throw new IllegalArgumentException("New password needs 8+ characters with a letter and a digit.");
        try (Connection con = jdbcconnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                int tokenId, userId;
                // FOR UPDATE locks the row so the same token cannot be used twice at the same moment.
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT token_id, user_id FROM password_reset_tokens "
                      + "WHERE reset_token = ? AND is_used = FALSE AND expiry_time > NOW() FOR UPDATE")) {
                    ps.setString(1, token.trim());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("Reset token is invalid, used or expired.");
                        tokenId = rs.getInt(1);
                        userId = rs.getInt(2);
                    }
                }
                try (PreparedStatement ps = con.prepareStatement("UPDATE users SET password_hash = ? WHERE user_id = ?")) {
                    ps.setString(1, PasswordManager.hashPassword(newPassword));
                    ps.setInt(2, userId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement("UPDATE password_reset_tokens SET is_used = TRUE WHERE token_id = ?")) {
                    ps.setInt(1, tokenId);
                    ps.executeUpdate();
                }
                AuditLogger.log(con, userId, "PASSWORD_RESET_COMPLETED", "USER", userId, null, null,
                        "Password reset with token");
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } catch (RuntimeException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    private static String newToken() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString(); // 32 hex characters
    }
}
