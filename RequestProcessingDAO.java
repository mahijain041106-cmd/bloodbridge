
package backend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RequestProcessingDAO {

    // Claim a queued request
    public boolean claimRequest(int requestId) throws SQLException {

        String sql =
                "UPDATE requests SET request_status = 'PROCESSING' " +
                "WHERE request_id = ? " +
                "AND validation_status = 'VALID' " +
                "AND request_status = 'QUEUED'";

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, requestId);

            return ps.executeUpdate() == 1;
        }
    }

    // Mark request unfulfilled and notify requester
    public boolean markUnfulfilled(int requestId) throws SQLException {

        long userId;

        String selectSQL =
                "SELECT requester_user_id FROM requests " +
                "WHERE request_id = ? " +
                "AND request_status = 'PROCESSING'";

        String updateSQL =
                "UPDATE requests SET request_status = 'UNFULFILLED' " +
                "WHERE request_id = ? " +
                "AND request_status = 'PROCESSING'";

        try (Connection con = jdbcconnection.getConnection()) {

            con.setAutoCommit(false);

            try {

                // Get requester ID
                try (PreparedStatement ps =
                             con.prepareStatement(selectSQL)) {

                    ps.setInt(1, requestId);

                    try (ResultSet rs = ps.executeQuery()) {

                        if (!rs.next()) {
                            con.rollback();
                            return false;
                        }

                        userId = rs.getLong("requester_user_id");
                    }
                }

                // Update request status
                try (PreparedStatement ps =
                             con.prepareStatement(updateSQL)) {

                    ps.setInt(1, requestId);

                    if (ps.executeUpdate() != 1) {
                        con.rollback();
                        return false;
                    }
                }

                con.commit();

            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }

        // Create notification after status update is committed
        try {

            NotificationRequest notification =
                    new NotificationRequest();

            notification.setUserId(userId);
            notification.setRequestId((long) requestId);
            notification.setReservationId(null);

            notification.setNotificationType("REQUEST_FAILED");

            notification.setTitle(
                    "Blood Request Could Not Be Fulfilled"
            );

            notification.setMessage(
                    "Your blood request (ID: " + requestId +
                    ") could not be fulfilled because sufficient " +
                    "compatible inventory was unavailable."
            );

            NotificationController controller =
                    new NotificationController();

            boolean created = controller.create(notification);

            if (!created) {
                System.out.println(
                        "Request status updated, but notification creation failed."
                );
            }

        } catch (Exception e) {

            System.out.println(
                    "Notification error: " + e.getMessage()
            );
        }

        return true;
    }

 // Mark successfully reserved request as fulfilled
 public boolean markFulfilled(int requestId) throws SQLException {

     String sql =
             "UPDATE requests SET request_status = 'FULFILLED' " +
             "WHERE request_id = ? " +
             "AND request_status = 'PROCESSING'";

     try (Connection con = jdbcconnection.getConnection();
          PreparedStatement ps = con.prepareStatement(sql)) {

         ps.setInt(1, requestId);

         return ps.executeUpdate() == 1;
     }
 }

public boolean queueRequest(int requestId) throws SQLException {

    String sql =
        "UPDATE requests SET request_status = 'QUEUED' " +
        "WHERE request_id = ? " +
        "AND validation_status = 'VALID' " +
        "AND request_status = 'SUBMITTED'";

    try (Connection con = jdbcconnection.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setInt(1, requestId);

        return ps.executeUpdate() == 1;
    }
}


}
