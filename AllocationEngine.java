
package backend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AllocationEngine {

    /**
     * Reads a request and finds eligible fictional inventory units.
     */
    public AllocationResult allocate(int requestId) {

        String group;
        String component;
        int quantity;

        String sql =
                "SELECT r.required_group_code, c.component_name, r.quantity "
                + "FROM requests r "
                + "JOIN components c ON c.component_id = r.component_id "
                + "WHERE r.request_id = ?";

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, requestId);

            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.next()) {
                    throw new IllegalArgumentException(
                            "Request " + requestId + " does not exist.");
                }

                group = rs.getString("required_group_code");
                component = rs.getString("component_name");
                quantity = rs.getInt("quantity");
            }

        } catch (SQLException e) {
            throw dbError(e);
        }

        AuditLogger.log(
                null,
                "ALLOCATION_STARTED",
                "REQUEST",
                requestId,
                null,
                AuditLogger.json(
                        "blood_group", group,
                        "component", component,
                        "quantity", String.valueOf(quantity)),
                "Matching started"
        );

        AllocationResult result =
                findEligibleUnits(group, component, quantity);

        logAttempt(requestId, result);

        return result;
    }

    /**
     * Finds eligible units using the currently active rule version.
     * This method does not reserve or update any inventory.
     */
    public AllocationResult findEligibleUnits(
            String recipientGroup,
            String component,
            int quantity) {

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be positive.");
        }

        String versionSql =
                "SELECT rule_version_id, version_code "
                + "FROM rule_versions "
                + "WHERE rule_status = 'ACTIVE' "
                + "AND (effective_from IS NULL OR effective_from <= NOW()) "
                + "AND (effective_to IS NULL OR effective_to >= NOW()) "
                + "ORDER BY version_number DESC LIMIT 1";

        try (Connection con = jdbcconnection.getConnection()) {

            int versionId;
            String versionName;

            try (PreparedStatement ps =
                         con.prepareStatement(versionSql);
                 ResultSet rs = ps.executeQuery()) {

                if (!rs.next()) {
                    throw new IllegalStateException(
                            "No active compatibility rule version found.");
                }

                versionId = rs.getInt("rule_version_id");
                versionName = rs.getString("version_code");
            }

            requireExists(
                    con,
                    "SELECT 1 FROM blood_groups "
                    + "WHERE blood_group_code = ?",
                    recipientGroup,
                    "Unknown blood group: "
            );

            requireExists(
                    con,
                    "SELECT 1 FROM components "
                    + "WHERE component_name = ?",
                    component,
                    "Unknown component: "
            );

            String matchingSql =
                    "SELECT bu.unit_id, bu.unit_code, "
                    + "bu.blood_group_code, bu.expiry_date "
                    + "FROM blood_units bu "
                    + "JOIN components c "
                    + "ON c.component_id = bu.component_id "
                    + "JOIN compatibility_rules cr "
                    + "ON cr.component_id = bu.component_id "
                    + "AND cr.unit_group_code = bu.blood_group_code "
                    + "WHERE cr.rule_version_id = ? "
                    + "AND cr.requested_group_code = ? "
                    + "AND c.component_name = ? "
                    + "AND cr.is_allowed = TRUE "
                    + "AND bu.unit_status = 'AVAILABLE' "
                    + "AND (bu.expiry_date IS NULL "
                    + "OR bu.expiry_date >= CURDATE()) "
                    + "ORDER BY bu.expiry_date ASC, bu.unit_id ASC";

            List<MatchCandidate> candidates =
                    new ArrayList<>();

            try (PreparedStatement ps =
                         con.prepareStatement(matchingSql)) {

                ps.setInt(1, versionId);
                ps.setString(2, recipientGroup);
                ps.setString(3, component);

                try (ResultSet rs = ps.executeQuery()) {

                    while (rs.next()) {

                        candidates.add(
                                new MatchCandidate(
                                        rs.getInt("unit_id"),
                                        rs.getString("unit_code"),
                                        rs.getString("blood_group_code"),
                                        rs.getDate("expiry_date"),
                                        1
                                )
                        );
                    }
                }
            }

            return new AllocationResult(
                    recipientGroup,
                    component,
                    quantity,
                    versionId,
                    versionName,
                    candidates
            );

        } catch (SQLException e) {
            throw dbError(e);
        }
    }

    /**
     * Records the matching attempt when the request is linked
     * to a batch_request. Also records the audit event.
     */
    private void logAttempt(int requestId, AllocationResult result) {

        try (Connection con = jdbcconnection.getConnection()) {

            int evaluatedCount = countAvailableUnits(con, result);

            String batchSql =
                    "SELECT batch_request_id "
                    + "FROM batch_requests "
                    + "WHERE request_id = ? "
                    + "ORDER BY batch_request_id DESC LIMIT 1";

            try (PreparedStatement ps =
                         con.prepareStatement(batchSql)) {

                ps.setInt(1, requestId);

                try (ResultSet rs = ps.executeQuery()) {

                    if (rs.next()) {

                        long batchRequestId =
                                rs.getLong("batch_request_id");

                        int attemptNumber =
                                getNextAttemptNumber(con, batchRequestId);

                        String attemptStatus;

                        if (result.isEnough()) {
                            attemptStatus = "MATCHED";
                        } else if (result.getCandidates().isEmpty()) {
                            attemptStatus = "NO_MATCH";
                        } else {
                            attemptStatus = "PARTIAL";
                        }

                        String insertSql =
                                "INSERT INTO allocation_attempts "
                                + "(batch_request_id, rule_version_id, "
                                + "attempt_number, attempt_status, "
                                + "evaluated_unit_count, eligible_unit_count, "
                                + "selected_unit_count, completed_at) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())";

                        try (PreparedStatement insert =
                                     con.prepareStatement(insertSql)) {

                            insert.setLong(1, batchRequestId);
                            insert.setInt(2, result.getRuleVersionId());
                            insert.setInt(3, attemptNumber);
                            insert.setString(4, attemptStatus);
                            insert.setInt(5, evaluatedCount);
                            insert.setInt(
                                    6, result.getCandidates().size());
                            insert.setInt(
                                    7, result.getSelectedUnits().size());

                            insert.executeUpdate();
                        }
                    }
                }
            }

            AuditLogger.log(
                    con,
                    null,
                    "ALLOCATION_COMPLETED",
                    "REQUEST",
                    requestId,
                    null,
                    AuditLogger.json(
                            "outcome", result.getOutcome(),
                            "candidates",
                            String.valueOf(result.getCandidates().size()),
                            "rule_version", result.getRuleVersionName()
                    ),
                    result.getOutcome() + ": "
                            + result.getCandidates().size()
                            + " eligible unit(s) for "
                            + result.getQuantity() + " requested"
            );

        } catch (SQLException e) {
            throw dbError(e);
        }
    }

    private int countAvailableUnits(
            Connection con, AllocationResult result)
            throws SQLException {

        String sql =
                "SELECT COUNT(*) "
                + "FROM blood_units bu "
                + "JOIN components c "
                + "ON c.component_id = bu.component_id "
                + "WHERE c.component_name = ? "
                + "AND bu.unit_status = 'AVAILABLE' "
                + "AND (bu.expiry_date IS NULL "
                + "OR bu.expiry_date >= CURDATE())";

        try (PreparedStatement ps =
                     con.prepareStatement(sql)) {

            ps.setString(1, result.getComponent());

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private int getNextAttemptNumber(
            Connection con, long batchRequestId)
            throws SQLException {

        String sql =
                "SELECT COALESCE(MAX(attempt_number), 0) + 1 "
                + "FROM allocation_attempts "
                + "WHERE batch_request_id = ?";

        try (PreparedStatement ps =
                     con.prepareStatement(sql)) {

            ps.setLong(1, batchRequestId);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static void requireExists(
            Connection con,
            String sql,
            String value,
            String message) throws SQLException {

        try (PreparedStatement ps =
                     con.prepareStatement(sql)) {

            ps.setString(1, value);

            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.next()) {
                    throw new IllegalArgumentException(
                            message + value);
                }
            }
        }
    }

    private static IllegalStateException dbError(SQLException e) {
        return new IllegalStateException(
                "Database error: " + e.getMessage(), e);
    }
}
