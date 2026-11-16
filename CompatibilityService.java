
package backend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CompatibilityService {

    // Add a compatibility rule
    public boolean add(
            long ruleVersionId,
            String requestedGroupCode,
            String unitGroupCode,
            int componentId,
            boolean allowed,
            String ruleNote) {

        String sql = """
                INSERT INTO compatibility_rules
                (rule_version_id, requested_group_code,
                 unit_group_code, component_id, is_allowed, rule_note)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, ruleVersionId);
            ps.setString(2, requestedGroupCode.trim());
            ps.setString(3, unitGroupCode.trim());
            ps.setInt(4, componentId);
            ps.setBoolean(5, allowed);
            ps.setString(6, ruleNote);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Compatibility rule add error: " + e.getMessage());
            return false;
        }
    }

    // Get all compatibility rules
    public List<CompatibilityRule> getAll() {
        String sql = """
                SELECT * FROM compatibility_rules
                ORDER BY rule_version_id, compatibility_rule_id
                """;

        List<CompatibilityRule> rules = new ArrayList<>();

        try (Connection con = jdbcconnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                rules.add(mapRule(rs));
            }

        } catch (SQLException e) {
            System.out.println("Compatibility rule retrieval error: " + e.getMessage());
        }

        return rules;
    }

    // Get rules belonging to a particular rule version
    public List<CompatibilityRule> getByVersion(long ruleVersionId) {
        String sql = """
                SELECT * FROM compatibility_rules
                WHERE rule_version_id = ?
                ORDER BY compatibility_rule_id
                """;

        List<CompatibilityRule> rules = new ArrayList<>();

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, ruleVersionId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rules.add(mapRule(rs));
                }
            }

        } catch (SQLException e) {
            System.out.println("Compatibility version search error: " + e.getMessage());
        }

        return rules;
    }

    // Get a rule by its ID
    public CompatibilityRule getById(long id) {
        String sql = """
                SELECT * FROM compatibility_rules
                WHERE compatibility_rule_id = ?
                """;

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRule(rs);
                }
            }

        } catch (SQLException e) {
            System.out.println("Compatibility rule search error: " + e.getMessage());
        }

        return null;
    }

    // Find whether a particular donor group is allowed
    // for the requested group, component and rule version
    public boolean isCompatible(
            long ruleVersionId,
            String requestedGroupCode,
            String unitGroupCode,
            int componentId) {

        String sql = """
                SELECT is_allowed
                FROM compatibility_rules
                WHERE rule_version_id = ?
                AND requested_group_code = ?
                AND unit_group_code = ?
                AND component_id = ?
                LIMIT 1
                """;

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, ruleVersionId);
            ps.setString(2, requestedGroupCode);
            ps.setString(3, unitGroupCode);
            ps.setInt(4, componentId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("is_allowed");
                }
            }

        } catch (SQLException e) {
            System.out.println("Compatibility check error: " + e.getMessage());
        }

        return false;
    }

    private CompatibilityRule mapRule(ResultSet rs) throws SQLException {
        return new CompatibilityRule(
                rs.getLong("compatibility_rule_id"),
                rs.getLong("rule_version_id"),
                rs.getString("requested_group_code"),
                rs.getString("unit_group_code"),
                rs.getInt("component_id"),
                rs.getBoolean("is_allowed"),
                rs.getString("rule_note"),
                rs.getTimestamp("created_at")
        );
    }
}
