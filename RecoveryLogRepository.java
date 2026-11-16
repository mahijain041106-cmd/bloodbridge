
package backend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class RecoveryLogRepository {

    public boolean save(RecoveryLog log) {
        String sql = """
                INSERT INTO recovery_logs
                (reservation_id, request_id, worker_id, recovery_type,
                 recovery_status, reason, attempt_count, detected_at, completed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            setNullableLong(ps, 1, log.getReservationId());
            setNullableLong(ps, 2, log.getRequestId());
            setNullableLong(ps, 3, log.getWorkerId());

            ps.setString(4, log.getRecoveryType());
            ps.setString(5, log.getRecoveryStatus());
            ps.setString(6, log.getReason());
            ps.setInt(7, log.getAttemptCount());

            if (log.getDetectedAt() == null) {
                ps.setTimestamp(8, new Timestamp(System.currentTimeMillis()));
            } else {
                ps.setTimestamp(8, Timestamp.valueOf(log.getDetectedAt()));
            }

            if (log.getCompletedAt() == null) {
                ps.setNull(9, java.sql.Types.TIMESTAMP);
            } else {
                ps.setTimestamp(9, Timestamp.valueOf(log.getCompletedAt()));
            }

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            System.out.println("Recovery log save error: " + e.getMessage());
            return false;
        }
    }

    public List<RecoveryLog> findAllByOrderByDetectedAtDesc() {
        String sql = """
                SELECT * FROM recovery_logs
                ORDER BY detected_at DESC
                """;

        List<RecoveryLog> logs = new ArrayList<>();

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                RecoveryLog log = new RecoveryLog();

                log.setRecoveryId(rs.getLong("recovery_id"));
                log.setReservationId(getNullableLong(rs, "reservation_id"));
                log.setRequestId(getNullableLong(rs, "request_id"));
                log.setWorkerId(getNullableLong(rs, "worker_id"));
                log.setRecoveryType(rs.getString("recovery_type"));
                log.setRecoveryStatus(rs.getString("recovery_status"));
                log.setReason(rs.getString("reason"));
                log.setAttemptCount(rs.getInt("attempt_count"));

                Timestamp detected = rs.getTimestamp("detected_at");
                if (detected != null) {
                    log.setDetectedAt(detected.toLocalDateTime());
                }

                Timestamp completed = rs.getTimestamp("completed_at");
                if (completed != null) {
                    log.setCompletedAt(completed.toLocalDateTime());
                }

                logs.add(log);
            }

        } catch (Exception e) {
            System.out.println("Recovery history error: " + e.getMessage());
        }

        return logs;
    }

    private void setNullableLong(
            PreparedStatement ps, int index, Long value) throws Exception {

        if (value == null) {
            ps.setNull(index, java.sql.Types.BIGINT);
        } else {
            ps.setLong(index, value);
        }
    }

    private Long getNullableLong(ResultSet rs, String column)
            throws Exception {

        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
