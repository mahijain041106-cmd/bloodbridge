
package backend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CompatibilityRuleRepository {

    // Find all compatibility rules
    public List<CompatibilityRule> findAll() {
        String sql = """
                SELECT *
                FROM compatibility_rules
                ORDER BY compatibility_rule_id
                """;

        List<CompatibilityRule> rules = new ArrayList<>();

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                rules.add(mapRule(rs));
            }

        } catch (Exception e) {
            System.out.println("Repository error: " + e.getMessage());
        }

        return rules;
    }

    // Find rules by rule version
    public List<CompatibilityRule> findByRuleVersionId(long ruleVersionId) {
        String sql = """
                SELECT *
                FROM compatibility_rules
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

        } catch (Exception e) {
            System.out.println("Rule lookup error: " + e.getMessage());
        }

        return rules;
    }

    // Find a rule by its ID
    public CompatibilityRule findById(long id) {
        String sql = """
                SELECT *
                FROM compatibility_rules
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

        } catch (Exception e) {
            System.out.println("Rule lookup error: " + e.getMessage());
        }

        return null;
    }

    // Check whether a specific rule already exists
    public boolean exists(
            long ruleVersionId,
            String requestedGroupCode,
            String unitGroupCode,
            int componentId) {

        String sql = """
                SELECT 1
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
                return rs.next();
            }

        } catch (Exception e) {
            System.out.println("Rule existence check error: " + e.getMessage());
            return false;
        }
    }

    private CompatibilityRule mapRule(ResultSet rs) throws Exception {
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
