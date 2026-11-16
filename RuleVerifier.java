
package backend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class RuleVerifier {

    public static void main(String[] args) {

        String sql = """
                SELECT
                    rv.rule_version_id,
                    rv.version_code,
                    COUNT(cr.compatibility_rule_id) AS rule_count
                FROM rule_versions rv
                LEFT JOIN compatibility_rules cr
                    ON rv.rule_version_id = cr.rule_version_id
                WHERE rv.rule_status = 'ACTIVE'
                GROUP BY rv.rule_version_id, rv.version_code
                ORDER BY rv.rule_version_id DESC
                """;

        int activeVersions = 0;

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.println("==================================");
            System.out.println(" BLOODBRIDGE RULE VERIFICATION");
            System.out.println("==================================");

            while (rs.next()) {
                activeVersions++;

                long versionId = rs.getLong("rule_version_id");
                String versionCode = rs.getString("version_code");
                int ruleCount = rs.getInt("rule_count");

                System.out.println("Rule Version: " + versionCode);
                System.out.println("Version ID: " + versionId);
                System.out.println("Configured Rules: " + ruleCount);

                if (ruleCount == 256) {
                    System.out.println("Result: All 256 combinations are present.");
                } else {
                    System.out.println("Result: Incomplete rule configuration.");
                    System.out.println("Missing combinations: " + (256 - ruleCount));
                }

                System.out.println("----------------------------------");
            }

            if (activeVersions == 0) {
                System.out.println("No active rule version found.");
            }

        } catch (Exception e) {
            System.out.println("Rule verification error: " + e.getMessage());
        }

        System.out.println("Verification finished.");
    }
}
