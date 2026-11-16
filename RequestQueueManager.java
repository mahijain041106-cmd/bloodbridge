package backend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Module 2: the ready queue and the scheduler.
 *
 * OS ideas shown here:
 *  - Ready queue:           rows with status WAITING in request_queue.
 *  - Priority scheduling:   the next request is the WAITING row with the highest priority.
 *  - FCFS tie-break:        among equal priorities, the smallest queue_id (earliest arrival) goes first.
 *  - Safe for many workers: dequeueNext() locks the chosen row, so two workers never get the same request.
 *
 * Boundary: this class only decides WHICH request goes next. Module 6 runs the workers.
 */
public class RequestQueueManager {

    /** Puts a SUBMITTED request into the queue. Its priority is copied from the requests table. */
    public int enqueue(int requestId) {
        try (Connection con = jdbcconnection.getConnection()) {
            con.setAutoCommit(false); // queue row + request status change happen together
            try {
                int priority;
                int requesterId;
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT priority, status, requester_user_id FROM requests WHERE request_id = ? FOR UPDATE")) {
                    ps.setInt(1, requestId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("Request " + requestId + " does not exist.");
                        String status = rs.getString("status");
                        if (!"SUBMITTED".equals(status))
                            throw new IllegalStateException("Only SUBMITTED requests can be queued (this one is " + status + ").");
                        priority = rs.getInt("priority");
                        requesterId = rs.getInt("requester_user_id");
                    }
                }
                int queueId;
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO request_queue (request_id, priority) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, requestId);
                    ps.setInt(2, priority);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        queueId = keys.getInt(1);
                    }
                }
                updateRequestStatus(con, requestId, "QUEUED");
                AuditLogger.log(con, requesterId, "REQUEST_QUEUED", "REQUEST", requestId,
                        AuditLogger.json("status", "SUBMITTED"), AuditLogger.json("status", "QUEUED"),
                        "Queued with priority " + priority + " (queue#" + queueId + ")");
                con.commit();
                return queueId;
            } catch (SQLException e) {
                con.rollback();
                if (e.getErrorCode() == 1062) throw new IllegalStateException("Request " + requestId + " is already in the queue.");
                throw e;
            } catch (RuntimeException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw dbError(e);
        }
    }

    /**
     * Picks and removes the next request: highest priority first, earliest arrival among equals.
     * Returns null if nothing is waiting.
     * FOR UPDATE SKIP LOCKED: if another worker is already taking the top row, this worker skips
     * it and takes the next one instead of waiting. Result: no request is handed out twice.
     */
    public QueueEntry dequeueNext() {
        try (Connection con = jdbcconnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                QueueEntry next;
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT queue_id, request_id, priority, status, enqueued_at FROM request_queue "
                      + "WHERE status = 'WAITING' ORDER BY priority DESC, queue_id ASC LIMIT 1 FOR UPDATE SKIP LOCKED");
                     ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) { con.rollback(); return null; }
                    next = readEntry(rs);
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE request_queue SET status = 'DISPATCHED', dispatched_at = NOW(3) WHERE queue_id = ?")) {
                    ps.setInt(1, next.getQueueId());
                    ps.executeUpdate();
                }
                updateRequestStatus(con, next.getRequestId(), "PROCESSING");
                AuditLogger.log(con, null, "REQUEST_DISPATCHED", "REQUEST", next.getRequestId(),
                        AuditLogger.json("status", "QUEUED"), AuditLogger.json("status", "PROCESSING"),
                        "Scheduler dispatched queue#" + next.getQueueId() + " (priority " + next.getPriority() + ")");
                con.commit();
                return next;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } catch (RuntimeException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw dbError(e);
        }
    }

    /** Shows who would go next without removing it (no lock; just a look). */
    public QueueEntry peekNext() {
        String sql = "SELECT queue_id, request_id, priority, status, enqueued_at FROM request_queue "
                   + "WHERE status = 'WAITING' ORDER BY priority DESC, queue_id ASC LIMIT 1";
        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? readEntry(rs) : null;
        } catch (SQLException e) {
            throw dbError(e);
        }
    }

    /** All waiting requests in the exact order they will be processed. */
    public List<QueueEntry> listWaiting() {
        List<QueueEntry> list = new ArrayList<>();
        String sql = "SELECT queue_id, request_id, priority, status, enqueued_at FROM request_queue "
                   + "WHERE status = 'WAITING' ORDER BY priority DESC, queue_id ASC";
        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(readEntry(rs));
            return list;
        } catch (SQLException e) {
            throw dbError(e);
        }
    }

    /**
     * Changes the priority of a request that is still WAITING.
     * queue_id is untouched, so among its new equals it keeps its original arrival position.
     */
    public void changePriority(int requestId, int newPriority, Integer actorUserId) {
        if (newPriority < 1 || newPriority > 5) throw new IllegalArgumentException("Priority must be 1 to 5.");
        try (Connection con = jdbcconnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                int oldPriority;
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT priority FROM request_queue WHERE request_id = ? AND status = 'WAITING' FOR UPDATE")) {
                    ps.setInt(1, requestId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalStateException("Request " + requestId + " is not waiting in the queue.");
                        oldPriority = rs.getInt(1);
                    }
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE request_queue SET priority = ? WHERE request_id = ? AND status = 'WAITING'")) {
                    ps.setInt(1, newPriority);
                    ps.setInt(2, requestId);
                    if (ps.executeUpdate() == 0) throw new IllegalStateException("Request " + requestId + " is not waiting in the queue.");
                }
                try (PreparedStatement ps = con.prepareStatement("UPDATE requests SET priority = ? WHERE request_id = ?")) {
                    ps.setInt(1, newPriority);
                    ps.setInt(2, requestId);
                    ps.executeUpdate();
                }
                AuditLogger.log(con, actorUserId, "REQUEST_PRIORITY_CHANGED", "REQUEST", requestId,
                        AuditLogger.json("priority", String.valueOf(oldPriority)),
                        AuditLogger.json("priority", String.valueOf(newPriority)), "Priority changed while waiting");
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } catch (RuntimeException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw dbError(e);
        }
    }

    /** Removes a WAITING request from the queue and marks the request CANCELLED. */
    public void cancel(int requestId, Integer actorUserId) {
        try (Connection con = jdbcconnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE request_queue SET status = 'REMOVED' WHERE request_id = ? AND status = 'WAITING'")) {
                    ps.setInt(1, requestId);
                    if (ps.executeUpdate() == 0) throw new IllegalStateException("Request " + requestId + " is not waiting in the queue.");
                }
                updateRequestStatus(con, requestId, "CANCELLED");
                AuditLogger.log(con, actorUserId, "REQUEST_CANCELLED", "REQUEST", requestId,
                        AuditLogger.json("status", "QUEUED"), AuditLogger.json("status", "CANCELLED"),
                        "Removed from the queue before processing");
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } catch (RuntimeException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw dbError(e);
        }
    }

    /** Called when processing of a dispatched request has finished (Module 6 will call this). */
    public void markCompleted(int requestId) {
        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                 "UPDATE request_queue SET status = 'DONE' WHERE request_id = ? AND status = 'DISPATCHED'")) {
            ps.setInt(1, requestId);
            if (ps.executeUpdate() == 0) throw new IllegalStateException("Request " + requestId + " was not dispatched.");
        } catch (SQLException e) {
            throw dbError(e);
        }
    }

    // ---------- helpers ----------

    private static void updateRequestStatus(Connection con, int requestId, String status) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE requests SET status = ? WHERE request_id = ?")) {
            ps.setString(1, status);
            ps.setInt(2, requestId);
            ps.executeUpdate();
        }
    }

    private static QueueEntry readEntry(ResultSet rs) throws SQLException {
        return new QueueEntry(rs.getInt("queue_id"), rs.getInt("request_id"), rs.getInt("priority"),
                              rs.getString("status"), rs.getTimestamp("enqueued_at"));
    }

    private static IllegalStateException dbError(SQLException e) {
        return new IllegalStateException("Database error: " + e.getMessage(), e);
    }
}
