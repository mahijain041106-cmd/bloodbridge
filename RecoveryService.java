
package backend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
//import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecoveryService {

    private final RecoveryLogRepository logs = new RecoveryLogRepository();

    public Map<String, Integer> cleanupExpiredReservations() {
        List<Long> expiredIds = new ArrayList<>();

        String sql = """
                SELECT reservation_id
                FROM reservations
                WHERE reservation_status = 'RESERVED'
                  AND expires_at IS NOT NULL
                  AND expires_at < CURRENT_TIMESTAMP
                """;

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                expiredIds.add(rs.getLong("reservation_id"));
            }

        } catch (Exception e) {
            System.out.println("Expired reservation search error: " + e.getMessage());
        }

        int recovered = 0;

        for (Long id : expiredIds) {
            if (recoverOne(id)) {
                recovered++;
            }
        }

        Map<String, Integer> result = new HashMap<>();
        result.put("found", expiredIds.size());
        result.put("recovered", recovered);

        return result;
    }

    public boolean recoverOne(Long reservationId) {
        String selectSql = """
                SELECT unit_id, request_id, worker_id
                FROM reservations
                WHERE reservation_id = ?
                  AND reservation_status = 'RESERVED'
                  AND expires_at IS NOT NULL
                  AND expires_at < CURRENT_TIMESTAMP
                FOR UPDATE
                """;

        String reservationSql = """
                UPDATE reservations
                SET reservation_status = 'EXPIRED',
                    released_at = CURRENT_TIMESTAMP,
                    release_reason = 'Temporary reservation expired'
                WHERE reservation_id = ?
                  AND reservation_status = 'RESERVED'
                """;

        String unitSql = """
                UPDATE blood_units
                SET unit_status = 'AVAILABLE',
                    updated_at = CURRENT_TIMESTAMP
                WHERE unit_id = ?
                  AND unit_status = 'RESERVED'
                """;

        String logSql = """
                INSERT INTO recovery_logs
                (reservation_id, request_id, worker_id,
                 recovery_type, recovery_status, reason,
                 attempt_count, detected_at, completed_at)
                VALUES (?, ?, ?, 'EXPIRED_RESERVATION',
                        'COMPLETED', ?, 1,
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """;

        try (Connection con = jdbcconnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                Long unitId;
                Long requestId;
                Long workerId;

                try (PreparedStatement ps = con.prepareStatement(selectSql)) {
                    ps.setLong(1, reservationId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            con.rollback();
                            return false;
                        }

                        unitId = rs.getLong("unit_id");
                        requestId = rs.getLong("request_id");

                        long worker = rs.getLong("worker_id");
                        workerId = rs.wasNull() ? null : worker;
                    }
                }

                try (PreparedStatement ps = con.prepareStatement(reservationSql)) {
                    ps.setLong(1, reservationId);

                    if (ps.executeUpdate() == 0) {
                        con.rollback();
                        return false;
                    }
                }

                try (PreparedStatement ps = con.prepareStatement(unitSql)) {
                    ps.setLong(1, unitId);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = con.prepareStatement(logSql)) {
                    ps.setLong(1, reservationId);
                    ps.setLong(2, requestId);

                    if (workerId == null) {
                        ps.setNull(3, Types.BIGINT);
                    } else {
                        ps.setLong(3, workerId);
                    }

                    ps.setString(4,
                            "Expired temporary reservation was released.");

                    ps.executeUpdate();
                }

                con.commit();
                return true;

            } catch (Exception e) {
                con.rollback();
                System.out.println("Recovery transaction error: " + e.getMessage());
                return false;

            } finally {
                con.setAutoCommit(true);
            }

        } catch (Exception e) {
            System.out.println("Recovery connection error: " + e.getMessage());
            return false;
        }
    }

    public List<RecoveryLog> getHistory() {
        return logs.findAllByOrderByDetectedAtDesc();
    }
}
