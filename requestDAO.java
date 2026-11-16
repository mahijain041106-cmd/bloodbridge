
package backend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class requestDAO {

    public boolean userExists(int userId) throws SQLException {

        String sql = "SELECT user_id FROM users "
                   + "WHERE user_id = ? AND is_active = 1";

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean bloodGroupExists(String groupCode)
            throws SQLException {

        String sql = "SELECT blood_group_code FROM blood_groups "
                   + "WHERE blood_group_code = ?";

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, groupCode);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean componentExists(int componentId)
            throws SQLException {

        String sql = "SELECT component_id FROM components "
                   + "WHERE component_id = ? AND is_active = 1";

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, componentId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public long insertRequest(request req) throws SQLException {

        String requestCode = "REQ-" +
                UUID.randomUUID().toString()
                        .substring(0, 12).toUpperCase();

        String sql = "INSERT INTO requests "
                + "(request_code, requester_user_id, "
                + "required_group_code, component_id, quantity, "
                + "priority_level, validation_status, "
                + "request_status, request_note, submitted_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, 'VALID', "
                + "'SUBMITTED', ?, CURRENT_TIMESTAMP)";

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, requestCode);
            ps.setInt(2, req.getRequesterUserId());
            ps.setString(3, req.getRequiredGroupCode());
            ps.setInt(4, req.getComponentId());
            ps.setInt(5, req.getQuantity());
            ps.setInt(6, req.getPriorityLevel());
            ps.setString(7, req.getRequestNote());

            int rows = ps.executeUpdate();

            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                }
            }
        }

        throw new SQLException("Request insertion failed!");
    }

    // Retrieve requests belonging to a particular user
    public List<RequestRecord> getRequestsByUser(int userId)
            throws SQLException {

        List<RequestRecord> requests = new ArrayList<>();

        String sql = "SELECT request_id, request_code, "
                + "required_group_code, component_id, quantity, "
                + "priority_level, validation_status, "
                + "request_status, request_note, submitted_at "
                + "FROM requests "
                + "WHERE requester_user_id = ? "
                + "ORDER BY submitted_at DESC";

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    RequestRecord record = new RequestRecord();

                    record.setRequestId(rs.getInt("request_id"));
                    record.setRequestCode(rs.getString("request_code"));
                    record.setBloodGroup(
                            rs.getString("required_group_code"));
                    record.setComponentId(rs.getInt("component_id"));
                    record.setQuantity(rs.getInt("quantity"));
                    record.setPriority(rs.getInt("priority_level"));
                    record.setValidationStatus(
                            rs.getString("validation_status"));
                    record.setRequestStatus(
                            rs.getString("request_status"));
                    record.setRequestNote(rs.getString("request_note"));
                    record.setSubmittedAt(
                            rs.getTimestamp("submitted_at"));

                    requests.add(record);
                }
            }
        }

        return requests;
    }

    // Retrieve all blood requests for admin
    public List<RequestRecord> getAllRequests()
            throws SQLException {

        List<RequestRecord> requests = new ArrayList<>();

        String sql = """
                SELECT
                    r.request_id,
                    r.request_code,
                    r.requester_user_id,
                    u.full_name AS requester_name,
                    r.required_group_code,
                    r.component_id,
                    r.quantity,
                    r.priority_level,
                    r.validation_status,
                    r.request_status,
                    r.request_note,
                    r.submitted_at
                FROM requests r
                JOIN users u
                    ON r.requester_user_id = u.user_id
                ORDER BY r.submitted_at DESC
                """;

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                RequestRecord record = new RequestRecord();

                record.setRequestId(rs.getInt("request_id"));
                record.setRequestCode(rs.getString("request_code"));

                record.setRequesterUserId(
                        rs.getInt("requester_user_id"));

                record.setRequesterName(
                        rs.getString("requester_name"));

                record.setBloodGroup(
                        rs.getString("required_group_code"));

                record.setComponentId(
                        rs.getInt("component_id"));

                record.setQuantity(rs.getInt("quantity"));

                record.setPriority(
                        rs.getInt("priority_level"));

                record.setValidationStatus(
                        rs.getString("validation_status"));

                record.setRequestStatus(
                        rs.getString("request_status"));

                record.setRequestNote(
                        rs.getString("request_note"));

                record.setSubmittedAt(
                        rs.getTimestamp("submitted_at"));

                requests.add(record);
            }
        }

        return requests;
    }

}
