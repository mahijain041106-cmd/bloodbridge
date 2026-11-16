package backend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Module 2 test: creates 5 fictional requests, queues them, then dispatches them one by one.
 * Needs at least one user (run Main --seed-admin first).
 * Arrival order and priority:  R1:3  R2:5  R3:3  R4:1  R5:5
 * Expected dispatch order:     R2, R5 (priority 5, FCFS), R1, R3 (priority 3, FCFS), R4
 *
 * Run: java -cp "out:lib/*" com.bloodbridge.requestqueue.RequestQueueDemo
 */
public class RequestQueueDemo {
    public static void main(String[] args) throws Exception {
        int[] priorities = {3, 5, 3, 1, 5};
        String[] groups = {"O-", "A+", "B+", "AB-", "O-"};
        RequestQueueManager queue = new RequestQueueManager();
        List<Integer> ids = new ArrayList<>();

        try (Connection con = jdbcconnection.getConnection()) {
            int userId;
            try (PreparedStatement ps = con.prepareStatement("SELECT user_id FROM users ORDER BY user_id LIMIT 1");
                 ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) { System.out.println("No users yet. Run Main --seed-admin first."); return; }
                userId = rs.getInt(1);
            }
            for (int i = 0; i < priorities.length; i++) {
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO requests (requester_user_id, blood_group, component, quantity, priority) "
                      + "VALUES (?, ?, 'RBC', 1, ?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, userId);
                    ps.setString(2, groups[i]);
                    ps.setInt(3, priorities[i]);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) { keys.next(); ids.add(keys.getInt(1)); }
                }
            }
        }

        System.out.println("Arrival order (request id: priority):");
        for (int i = 0; i < ids.size(); i++) {
            queue.enqueue(ids.get(i));
            System.out.println("  request#" + ids.get(i) + ": " + priorities[i]);
        }

        System.out.println("\nQueue as the scheduler sees it:");
        for (QueueEntry e : queue.listWaiting()) System.out.println("  " + e);

        System.out.println("\nDispatching:");
        QueueEntry next;
        while ((next = queue.dequeueNext()) != null) {
            System.out.println("  -> " + next);
            queue.markCompleted(next.getRequestId());
        }
        System.out.println("\nQueue empty. Check that the order matches the expected order above.");
    }
}
