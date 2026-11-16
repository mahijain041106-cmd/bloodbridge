
package backend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AuditManager {

    private static final String SELECT =
            "SELECT a.audit_id, a.created_at, "
            + "COALESCE(u.username, 'system') AS actor, "
            + "a.action_type, a.entity_type, a.entity_id, "
            + "a.old_values, a.new_values, a.details "
            + "FROM audit_logs a "
            + "LEFT JOIN users u ON u.user_id = a.actor_user_id ";

    public List<AuditEntry> getRecent(AuthManager auth, int limit) {
        requireAdmin(auth);
        return query(
                SELECT + "ORDER BY a.audit_id DESC LIMIT ?", limit);
    }

    public List<AuditEntry> getForEntity(
            AuthManager auth, String entityType, int entityId) {

        requireAdmin(auth);

        return query(
                SELECT + "WHERE a.entity_type = ? "
                + "AND a.entity_id = ? "
                + "ORDER BY a.audit_id ASC",
                entityType, entityId);
    }

    public List<AuditEntry> getByAction(
            AuthManager auth, String actionType, int limit) {

        requireAdmin(auth);

        return query(
                SELECT + "WHERE a.action_type = ? "
                + "ORDER BY a.audit_id DESC LIMIT ?",
                actionType, limit);
    }

    public List<AuditEntry> getMyHistory(
            AuthManager auth, int limit) {

        if (!auth.isLoggedIn()) {
            throw new IllegalStateException("Log in first.");
        }

        return query(
                SELECT + "WHERE a.actor_user_id = ? "
                + "ORDER BY a.audit_id DESC LIMIT ?",
                auth.getCurrentUser().getUserId(), limit);
    }

    private static void requireAdmin(AuthManager auth) {
        if (!auth.hasRole("ADMIN")) {
            throw new IllegalStateException(
                    "Only an ADMIN can view the full audit log.");
        }
    }

    private static List<AuditEntry> query(
            String sql, Object... params) {

        List<AuditEntry> list = new ArrayList<>();

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                if (params[i] instanceof Integer) {
                    ps.setInt(i + 1, (Integer) params[i]);
                } else {
                    ps.setString(i + 1, (String) params[i]);
                }
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {

                    int entityId = rs.getInt("entity_id");
                    Integer entity = rs.wasNull() ? null : entityId;

                    list.add(new AuditEntry(
                            rs.getLong("audit_id"),
                            rs.getTimestamp("created_at"),
                            rs.getString("actor"),
                            rs.getString("action_type"),
                            rs.getString("entity_type"),
                            entity,
                            rs.getString("old_values"),
                            rs.getString("new_values"),
                            rs.getString("details")
                    ));
                }
            }

            return list;

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Database error: " + e.getMessage(), e);
        }
    }
}
