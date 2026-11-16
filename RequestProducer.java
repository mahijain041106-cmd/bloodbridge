
package backend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class RequestProducer implements Runnable {

    @Override
    public void run() {

        List<Integer> requestIds = new ArrayList<>();

        String selectSQL =
                "SELECT request_id FROM requests " +
                "WHERE validation_status = 'VALID' " +
                "AND request_status='SUBMITTED' " +
                "ORDER BY priority_level DESC, created_at ASC";

        String updateSQL =
                "UPDATE requests SET request_status = 'QUEUED' " +
                "WHERE request_id = ? " +
                "AND request_status = 'SUBMITTED'";

        try (Connection con = jdbcconnection.getConnection()) {

            // Step 1: Fetch pending requests
            try (PreparedStatement ps =
                         con.prepareStatement(selectSQL);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    requestIds.add(rs.getInt("request_id"));
                }
            }

            // Step 2: Add requests to processing queue
            for (int requestId : requestIds) {

                try (PreparedStatement ps =
                             con.prepareStatement(updateSQL)) {

                    ps.setInt(1, requestId);
                    ps.executeUpdate();
                }

                RequestQueue.addRequest(requestId);

                System.out.println(
                        "Producer added Request " +
                        requestId + " to queue.");
            }

        } catch (Exception e) {

            System.out.println(
                    "Producer error: " + e.getMessage());

            e.printStackTrace();
        }
    }
}
