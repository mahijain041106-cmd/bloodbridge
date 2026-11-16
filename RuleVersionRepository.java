
package backend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class RuleVersionRepository {

    // Get all rule versions
    public List<RuleVersion> findAll() {
        String sql = """
                SELECT *
                FROM rule_versions
                ORDER BY version_number DESC
                """;

        List<RuleVersion> versions = new ArrayList<>();

        try (Connection con = jdbcconnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                versions.add(mapVersion(rs));
            }

        } catch (Exception e) {
            System.out.println("Rule version retrieval error: " + e.getMessage());
        }

        return versions;
    }

    // Get active rule versions
    public List<RuleVersion> findActive() {
        String sql = """
                SELECT *
                FROM rule_versions
                WHERE rule_status = 'ACTIVE'
                ORDER BY version_number DESC
                """;

        List<RuleVersion> versions = new ArrayList<>();

        try (Connection con = jdbcconnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                versions.add(mapVersion(rs));
            }

        } catch (Exception e) {
            System.out.println("Active rule retrieval error: " + e.getMessage());
        }

        return versions;
    }

    // Find a rule version by ID
    public RuleVersion findById(long id) {
        String sql = """
                SELECT *
                FROM rule_versions
                WHERE rule_version_id = ?
                """;

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapVersion(rs);
                }
            }

        } catch (Exception e) {
            System.out.println("Rule version search error: " + e.getMessage());
        }

        return null;
    }

    private RuleVersion mapVersion(ResultSet rs) throws Exception {
        long createdBy = rs.getLong("created_by_user_id");
        Long createdById = rs.wasNull() ? null : createdBy;

        long approvedBy = rs.getLong("approved_by_user_id");
        Long approvedById = rs.wasNull() ? null : approvedBy;

        return new RuleVersion(
                rs.getLong("rule_version_id"),
                rs.getString("version_code"),
                rs.getInt("version_number"),
                rs.getString("source_description"),
                rs.getString("approval_note"),
                rs.getString("rule_status"),
                createdById,
                approvedById,
                rs.getTimestamp("effective_from"),
                rs.getTimestamp("effective_to"),
                rs.getTimestamp("created_at")
        );
    }
}
