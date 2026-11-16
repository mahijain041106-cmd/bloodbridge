
package backend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class AuditLogger {

    private static final String INSERT =
        "INSERT INTO audit_logs " +
        "(actor_user_id, action_type, entity_type, entity_id, " +
        "old_values, new_values, details) " +
        "VALUES (?, ?, ?, ?, CAST(? AS JSON), CAST(? AS JSON), ?)";

    // Log using an existing database transaction
    public static void log(
            Connection con,
            Integer actorUserId,
            String actionType,
            String entityType,
            Integer entityId,
            String oldValues,
            String newValues,
            String details) throws SQLException {

        try (PreparedStatement ps = con.prepareStatement(INSERT)) {

            setInt(ps, 1, actorUserId);
            ps.setString(2, actionType);
            ps.setString(3, entityType);
            setInt(ps, 4, entityId);
            setText(ps, 5, oldValues);
            setText(ps, 6, newValues);

            setText(ps, 7,
                details == null ? null :
                (details.length() > 500 ? details.substring(0, 500) : details));

            ps.executeUpdate();
        }
    }

    // Log using the single JDBC connection class
    public static void log(
            Integer actorUserId,
            String actionType,
            String entityType,
            Integer entityId,
            String oldValues,
            String newValues,
            String details) {

        try (Connection con = jdbcconnection.getConnection()) {

            log(con, actorUserId, actionType, entityType,
                entityId, oldValues, newValues, details);

        } catch (SQLException e) {

            throw new IllegalStateException(
                "Could not write audit record: " + e.getMessage(), e);
        }
    }

    public static String json(String... keysAndValues) {

        if (keysAndValues.length % 2 != 0) {
            throw new IllegalArgumentException(
                "json() needs key/value pairs");
        }

        StringBuilder sb = new StringBuilder("{");

        for (int i = 0; i < keysAndValues.length; i += 2) {

            if (i > 0) sb.append(',');

            sb.append('"')
              .append(escape(keysAndValues[i]))
              .append("\":");

            String value = keysAndValues[i + 1];

            if (value == null) {
                sb.append("null");
            } else {
                sb.append('"')
                  .append(escape(value))
                  .append('"');
            }
        }

        return sb.append('}').toString();
    }

    private static String escape(String s) {

        StringBuilder sb = new StringBuilder();

        for (char c : s.toCharArray()) {

            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;

                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }

        return sb.toString();
    }

    private static void setInt(
            PreparedStatement ps, int index, Integer value)
            throws SQLException {

        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    private static void setText(
            PreparedStatement ps, int index, String value)
            throws SQLException {

        if (value == null) {
            ps.setNull(index, Types.VARCHAR);
        } else {
            ps.setString(index, value);
        }
    }
}
