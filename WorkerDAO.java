
package backend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class WorkerDAO {

    // Register a worker or reuse an existing worker
    public long registerWorker(
            String workerCode,
            String processId,
            String threadId) throws Exception {

        String sql =
                "INSERT INTO processing_workers " +
                "(worker_code, process_identifier, thread_identifier, " +
                "worker_status, started_at, last_heartbeat_at, stopped_at) " +
                "VALUES (?, ?, ?, 'RUNNING', CURRENT_TIMESTAMP, " +
                "CURRENT_TIMESTAMP, NULL) " +
                "ON DUPLICATE KEY UPDATE " +
                "worker_id = LAST_INSERT_ID(worker_id), " +
                "process_identifier = VALUES(process_identifier), " +
                "thread_identifier = VALUES(thread_identifier), " +
                "worker_status = 'RUNNING', " +
                "started_at = CURRENT_TIMESTAMP, " +
                "last_heartbeat_at = CURRENT_TIMESTAMP, " +
                "stopped_at = NULL";

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, workerCode);
            ps.setString(2, processId);
            ps.setString(3, threadId);

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }

        throw new Exception("Worker registration failed.");
    }

    // Update worker status
    public void updateWorkerStatus(
            long workerId,
            String status) throws Exception {

        String sql =
                "UPDATE processing_workers " +
                "SET worker_status = ?, " +
                "last_heartbeat_at = CURRENT_TIMESTAMP, " +
                "stopped_at = CASE " +
                "WHEN ? = 'STOPPED' THEN CURRENT_TIMESTAMP " +
                "ELSE NULL END " +
                "WHERE worker_id = ?";

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setString(2, status);
            ps.setLong(3, workerId);

            ps.executeUpdate();
        }
    }
}
