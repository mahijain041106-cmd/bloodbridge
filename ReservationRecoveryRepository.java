
package backend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRecoveryRepository {

    public List<Long> findExpiredIds() {
        String sql = """
                SELECT reservation_id
                FROM reservations
                WHERE reservation_status = 'RESERVED'
                  AND expires_at IS NOT NULL
                  AND expires_at < CURRENT_TIMESTAMP
                """;

        List<Long> ids = new ArrayList<>();

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                ids.add(rs.getLong("reservation_id"));
            }

        } catch (Exception e) {
            System.out.println("Expired reservation lookup error: " + e.getMessage());
        }

        return ids;
    }

    public Optional<RecoveryReservation> findByIdForUpdate(
            Connection con, Long reservationId) {

        String sql = """
                SELECT reservation_id, request_id, unit_id, worker_id,
                       reservation_status, expires_at
                FROM reservations
                WHERE reservation_id = ?
                FOR UPDATE
                """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, reservationId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    RecoveryReservation reservation = new RecoveryReservation();

                    reservation.setId(rs.getLong("reservation_id"));
                    reservation.setRequestId(rs.getLong("request_id"));
                    reservation.setUnitId(rs.getLong("unit_id"));

                    long workerId = rs.getLong("worker_id");
                    reservation.setWorkerId(rs.wasNull() ? null : workerId);

                    reservation.setStatus(rs.getString("reservation_status"));

                    Timestamp expiry = rs.getTimestamp("expires_at");
                    if (expiry != null) {
                        reservation.setExpiresAt(expiry.toLocalDateTime());
                    }

                    return Optional.of(reservation);
                }
            }

        } catch (Exception e) {
            System.out.println("Reservation recovery lookup error: " + e.getMessage());
        }

        return Optional.empty();
    }
}
