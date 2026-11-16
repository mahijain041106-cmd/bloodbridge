
package backend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class ReservationDAO {

    public boolean reserveUnit(int requestId, long workerId) {

        Connection con = null;

        try {
            con = jdbcconnection.getConnection();

            if (con == null) {
                throw new SQLException("Database connection failed.");
            }

            con.setAutoCommit(false);

            // 1. Get request details
            String requestSQL =
                "SELECT required_group_code, component_id, quantity " +
                "FROM requests " +
                "WHERE request_id = ? AND request_status = 'PROCESSING'";

            String bloodGroup;
            int componentId;
            int quantity;

            try (PreparedStatement ps = con.prepareStatement(requestSQL)) {

                ps.setInt(1, requestId);

                try (ResultSet rs = ps.executeQuery()) {

                    if (!rs.next()) {
                        throw new SQLException(
                            "Request not found or not processing."
                        );
                    }

                    bloodGroup = rs.getString("required_group_code");
                    componentId = rs.getInt("component_id");
                    quantity = rs.getInt("quantity");
                }
            }

            // 2. Lock eligible available inventory units
            String unitSQL =
                "SELECT unit_id FROM blood_units " +
                "WHERE blood_group_code = ? " +
                "AND component_id = ? " +
                "AND unit_status = 'AVAILABLE' " +
                "AND expiry_date >= CURDATE() " +
                "ORDER BY expiry_date ASC " +
                "LIMIT " + quantity + " FOR UPDATE";

            long[] unitIds = new long[quantity];
            int count = 0;

            try (PreparedStatement ps = con.prepareStatement(unitSQL)) {

                ps.setString(1, bloodGroup);
                ps.setInt(2, componentId);

                try (ResultSet rs = ps.executeQuery()) {

                    while (rs.next()) {
                        unitIds[count++] = rs.getLong("unit_id");
                    }
                }
            }

            // 3. Check availability
            if (count < quantity) {
                con.rollback();

                System.out.println(
                    "Insufficient inventory for Request " + requestId
                );

                return false;
            }

            // 4. Prepare reservation statements
            String updateSQL =
                "UPDATE blood_units " +
                "SET unit_status = 'RESERVED' " +
                "WHERE unit_id = ? AND unit_status = 'AVAILABLE'";

            String insertSQL =
                "INSERT INTO reservations " +
                "(reservation_code, request_id, unit_id, rule_version_id, " +
                "reservation_status, reserved_by_user_id, worker_id, " +
                "reserved_at, expires_at) " +
                "VALUES (?, ?, ?, " +
                "(SELECT rule_version_id FROM rule_versions " +
                "WHERE rule_status = 'ACTIVE' " +
                "AND (effective_from IS NULL OR effective_from <= CURRENT_TIMESTAMP) " +
                "AND (effective_to IS NULL OR effective_to >= CURRENT_TIMESTAMP) " +
                "ORDER BY version_number DESC LIMIT 1), " +
                "'RESERVED', " +
                "(SELECT requester_user_id FROM requests WHERE request_id = ?), " +
                "?, CURRENT_TIMESTAMP, " +
                "DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 15 MINUTE))";

            try (
                PreparedStatement updatePS =
                    con.prepareStatement(updateSQL);

                PreparedStatement insertPS =
                    con.prepareStatement(insertSQL)
            ) {

                for (long unitId : unitIds) {

                    updatePS.setLong(1, unitId);

                    if (updatePS.executeUpdate() != 1) {
                        throw new SQLException(
                            "Unit could not be reserved: " + unitId
                        );
                    }

                    String reservationCode =
                        "RES-" + UUID.randomUUID()
                        .toString().substring(0, 12).toUpperCase();

                    insertPS.setString(1, reservationCode);
                    insertPS.setInt(2, requestId);
                    insertPS.setLong(3, unitId);
                    insertPS.setInt(4, requestId);
                    insertPS.setLong(5, workerId);

                    insertPS.executeUpdate();
                }
            }

            // 5. Create notification for the requester
            String notificationSQL =
                "INSERT INTO notifications " +
                "(user_id, request_id, notification_type, title, message, is_read) " +
                "SELECT requester_user_id, request_id, " +
                "'REQUEST_STATUS', " +
                "'Blood availability update', ?, FALSE " +
                "FROM requests WHERE request_id = ?";

            try (PreparedStatement ps =
                    con.prepareStatement(notificationSQL)) {

                ps.setString(
                    1,
                    "Compatible inventory is available. " +
                    quantity + " unit(s) have been temporarily reserved " +
                    "for your request."
                );

                ps.setInt(2, requestId);

                if (ps.executeUpdate() != 1) {
                    throw new SQLException(
                        "Notification could not be created."
                    );
                }
            }

            // 6. Commit reservations and notification together
            con.commit();

            System.out.println(
                "Reservation successful for Request " + requestId
            );

            System.out.println(
                "Requester notification created successfully."
            );

            return true;

        } catch (Exception e) {

            try {
                if (con != null) {
                    con.rollback();
                }
            } catch (SQLException rollbackError) {
                System.out.println(
                    "Rollback failed: " + rollbackError.getMessage()
                );
            }

            System.out.println(
                "Reservation failed: " + e.getMessage()
            );

            return false;

        } finally {

            try {
                if (con != null) {
                    con.setAutoCommit(true);
                    con.close();
                }
            } catch (SQLException e) {
                System.out.println(
                    "Connection closing error: " + e.getMessage()
                );
            }
        }
    }
}
