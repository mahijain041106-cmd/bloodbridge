
package backend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class ReservationService {

    // Confirm a reservation
    public boolean confirmReservation(long reservationId) {

        String sql =
            "UPDATE reservations " +
            "SET reservation_status = 'CONFIRMED', " +
            "confirmed_at = CURRENT_TIMESTAMP " +
            "WHERE reservation_id = ? " +
            "AND reservation_status = 'RESERVED' " +
            "AND expires_at > CURRENT_TIMESTAMP";

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, reservationId);

            int updated = ps.executeUpdate();

            if (updated == 1) {
                System.out.println(
                    "Reservation " + reservationId +
                    " confirmed successfully."
                );
                return true;
            }

            System.out.println(
                "Confirmation failed. Reservation may be expired " +
                "or not in RESERVED status."
            );

            return false;

        } catch (SQLException e) {

            System.out.println(
                "Confirmation error: " + e.getMessage()
            );

            return false;
        }
    }

    // Cancel reservation and release inventory
    public boolean cancelReservation(long reservationId) {

        Connection con = null;

        try {
            con = jdbcconnection.getConnection();
            con.setAutoCommit(false);

            long unitId;

            // Retrieve and lock reservation
            String selectSQL =
                "SELECT unit_id, reservation_status " +
                "FROM reservations " +
                "WHERE reservation_id = ? FOR UPDATE";

            try (PreparedStatement ps = con.prepareStatement(selectSQL)) {

                ps.setLong(1, reservationId);

                try (ResultSet rs = ps.executeQuery()) {

                    if (!rs.next()) {
                        throw new SQLException("Reservation not found.");
                    }

                    if (!"RESERVED".equals(
                            rs.getString("reservation_status"))) {

                        throw new SQLException(
                            "Only RESERVED reservations can be cancelled."
                        );
                    }

                    unitId = rs.getLong("unit_id");
                }
            }

            // Update reservation status
            String cancelSQL =
                "UPDATE reservations " +
                "SET reservation_status = 'CANCELLED', " +
                "released_at = CURRENT_TIMESTAMP, " +
                "release_reason = 'Cancelled by user' " +
                "WHERE reservation_id = ? " +
                "AND reservation_status = 'RESERVED'";

            try (PreparedStatement ps = con.prepareStatement(cancelSQL)) {

                ps.setLong(1, reservationId);

                if (ps.executeUpdate() != 1) {
                    throw new SQLException(
                        "Reservation cancellation failed."
                    );
                }
            }

            // Release inventory unit
            String releaseSQL =
                "UPDATE blood_units " +
                "SET unit_status = 'AVAILABLE' " +
                "WHERE unit_id = ? " +
                "AND unit_status = 'RESERVED'";

            try (PreparedStatement ps = con.prepareStatement(releaseSQL)) {

                ps.setLong(1, unitId);

                if (ps.executeUpdate() != 1) {
                    throw new SQLException(
                        "Inventory release failed."
                    );
                }
            }

            con.commit();

            System.out.println(
                "Reservation cancelled and inventory released."
            );

            return true;

        } catch (Exception e) {

            try {
                if (con != null) {
                    con.rollback();
                }
            } catch (SQLException rollbackError) {
                System.out.println("Rollback failed.");
            }

            System.out.println(
                "Cancellation error: " + e.getMessage()
            );

            return false;

        } finally {

            try {
                if (con != null) {
                    con.setAutoCommit(true);
                    con.close();
                }
            } catch (SQLException e) {
                System.out.println("Connection closing error.");
            }
        }
    }
 // Automatically expire reservations and release inventory
    public int expireReservations() {

        Connection con = null;
        int expiredCount = 0;

        try {
            con = jdbcconnection.getConnection();
            con.setAutoCommit(false);

            List<Long> reservationIds = new ArrayList<>();
            List<Long> unitIds = new ArrayList<>();

            String selectSQL =
                "SELECT reservation_id, unit_id FROM reservations " +
                "WHERE reservation_status = 'RESERVED' " +
                "AND expires_at <= CURRENT_TIMESTAMP FOR UPDATE";

            try (PreparedStatement ps = con.prepareStatement(selectSQL);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    reservationIds.add(rs.getLong("reservation_id"));
                    unitIds.add(rs.getLong("unit_id"));
                }
            }

            for (int i = 0; i < reservationIds.size(); i++) {

                String updateReservation =
                    "UPDATE reservations SET " +
                    "reservation_status = 'EXPIRED', " +
                    "released_at = CURRENT_TIMESTAMP, " +
                    "release_reason = 'Reservation expired' " +
                    "WHERE reservation_id = ? " +
                    "AND reservation_status = 'RESERVED'";

                try (PreparedStatement ps =
                         con.prepareStatement(updateReservation)) {

                    ps.setLong(1, reservationIds.get(i));

                    if (ps.executeUpdate() == 1) {

                        String releaseUnit =
                            "UPDATE blood_units SET " +
                            "unit_status = CASE " +
                            "WHEN expiry_date < CURDATE() THEN 'EXPIRED' " +
                            "ELSE 'AVAILABLE' END " +
                            "WHERE unit_id = ? AND unit_status = 'RESERVED'";

                        try (PreparedStatement release =
                                 con.prepareStatement(releaseUnit)) {

                            release.setLong(1, unitIds.get(i));
                            release.executeUpdate();
                        }

                        expiredCount++;
                    }
                }
            }

            con.commit();

            System.out.println(
                expiredCount + " expired reservations processed."
            );

            return expiredCount;

        } catch (Exception e) {

            try {
                if (con != null) {
                    con.rollback();
                }
            } catch (SQLException ex) {
                System.out.println("Rollback failed.");
            }

            System.out.println("Expiry processing error: " + e.getMessage());
            return 0;

        } finally {

            try {
                if (con != null) {
                    con.setAutoCommit(true);
                    con.close();
                }
            } catch (SQLException e) {
                System.out.println("Connection closing error.");
            }
        }
    }
 // Retrieve all reservations for management portal
    public List<Map<String, Object>> getAllReservations() throws SQLException {

        List<Map<String, Object>> reservations = new ArrayList<>();

        String sql = """
                SELECT
                    r.reservation_id,
                    r.reservation_code,
                    r.request_id,
                    req.request_code,
                    req.requester_user_id,
                    u.full_name AS requester_name,
                    b.unit_code,
                    b.blood_group_code,
                    req.component_id,
                    req.quantity,
                    r.reservation_status,
                    r.reserved_at,
                    r.expires_at,
                    r.confirmed_at,
                    r.released_at
                FROM reservations r
                JOIN requests req
                    ON r.request_id = req.request_id
                JOIN users u
                    ON req.requester_user_id = u.user_id
                JOIN blood_units b
                    ON r.unit_id = b.unit_id
                ORDER BY r.reserved_at DESC
                """;

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();

                row.put("reservationId", rs.getLong("reservation_id"));
                row.put("reservationCode", rs.getString("reservation_code"));
                row.put("requestId", rs.getLong("request_id"));
                row.put("requestCode", rs.getString("request_code"));
                row.put("requesterName", rs.getString("requester_name"));
                row.put("unitCode", rs.getString("unit_code"));
                row.put("bloodGroup", rs.getString("blood_group_code"));
                row.put("componentId", rs.getInt("component_id"));
                row.put("quantity", rs.getInt("quantity"));
                row.put("status", rs.getString("reservation_status"));
                row.put("reservedAt", rs.getTimestamp("reserved_at"));
                row.put("expiresAt", rs.getTimestamp("expires_at"));
                row.put("confirmedAt", rs.getTimestamp("confirmed_at"));
                row.put("releasedAt", rs.getTimestamp("released_at"));

                reservations.add(row);
            }
        }

        return reservations;
    }
}
