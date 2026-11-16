
package backend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NotificationRepository {

    public boolean save(NotificationRecord notification) {
        String sql = """
                INSERT INTO notifications
                (user_id, request_id, reservation_id, notification_type,
                 title, message, is_read)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, notification.getUserId());
            setNullableLong(ps, 2, notification.getRequestId());
            setNullableLong(ps, 3, notification.getReservationId());
            ps.setString(4, notification.getNotificationType());
            ps.setString(5, notification.getTitle());
            ps.setString(6, notification.getMessage());
            ps.setBoolean(7, notification.isRead());

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            System.out.println("Notification save error: " + e.getMessage());
            return false;
        }
    }

    public List<NotificationRecord> findAll() {
        return findByQuery("SELECT * FROM notifications ORDER BY created_at DESC");
    }

    public List<NotificationRecord> findByUserId(Long userId) {
        String sql = """
                SELECT * FROM notifications
                WHERE user_id = ?
                ORDER BY created_at DESC
                """;

        return findByUserQuery(sql, userId, null);
    }

    public List<NotificationRecord> findByUserIdAndRead(
            Long userId, boolean read) {

        String sql = """
                SELECT * FROM notifications
                WHERE user_id = ? AND is_read = ?
                ORDER BY created_at DESC
                """;

        return findByUserQuery(sql, userId, read);
    }

    public Optional<NotificationRecord> findById(Long id) {
        String sql = """
                SELECT * FROM notifications
                WHERE notification_id = ?
                """;

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapNotification(rs));
                }
            }

        } catch (Exception e) {
            System.out.println("Notification lookup error: " + e.getMessage());
        }

        return Optional.empty();
    }

    public boolean markRead(Long id, boolean read) {
        String sql = """
                UPDATE notifications
                SET is_read = ?,
                    read_at = CASE
                        WHEN ? = TRUE THEN CURRENT_TIMESTAMP
                        ELSE NULL
                    END
                WHERE notification_id = ?
                """;

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setBoolean(1, read);
            ps.setBoolean(2, read);
            ps.setLong(3, id);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            System.out.println("Notification update error: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteById(Long id) {
        String sql = """
                DELETE FROM notifications
                WHERE notification_id = ?
                """;

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            System.out.println("Notification deletion error: " + e.getMessage());
            return false;
        }
    }

    private List<NotificationRecord> findByQuery(String sql) {
        List<NotificationRecord> notifications = new ArrayList<>();

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                notifications.add(mapNotification(rs));
            }

        } catch (Exception e) {
            System.out.println("Notification retrieval error: " + e.getMessage());
        }

        return notifications;
    }

    private List<NotificationRecord> findByUserQuery(
            String sql, Long userId, Boolean read) {

        List<NotificationRecord> notifications = new ArrayList<>();

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);

            if (read != null) {
                ps.setBoolean(2, read);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapNotification(rs));
                }
            }

        } catch (Exception e) {
            System.out.println("User notification retrieval error: " + e.getMessage());
        }

        return notifications;
    }

    private NotificationRecord mapNotification(ResultSet rs) throws Exception {
        NotificationRecord notification = new NotificationRecord();

        notification.setNotificationId(rs.getLong("notification_id"));
        notification.setUserId(rs.getLong("user_id"));

        notification.setRequestId(getNullableLong(rs, "request_id"));
        notification.setReservationId(getNullableLong(rs, "reservation_id"));

        notification.setNotificationType(rs.getString("notification_type"));
        notification.setTitle(rs.getString("title"));
        notification.setMessage(rs.getString("message"));
        notification.setRead(rs.getBoolean("is_read"));

        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            notification.setCreatedAt(created.toLocalDateTime());
        }

        Timestamp readAt = rs.getTimestamp("read_at");
        if (readAt != null) {
            notification.setReadAt(readAt.toLocalDateTime());
        }

        return notification;
    }

    private Long getNullableLong(ResultSet rs, String column)
            throws Exception {

        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private void setNullableLong(
            PreparedStatement ps, int index, Long value) throws Exception {

        if (value == null) {
            ps.setNull(index, Types.BIGINT);
        } else {
            ps.setLong(index, value);
        }
    }

public boolean markRead(Long id, Long userId, boolean read) {
    String sql = """
            UPDATE notifications
            SET is_read = ?,
                read_at = CASE
                    WHEN ? = TRUE THEN CURRENT_TIMESTAMP
                    ELSE NULL
                END
            WHERE notification_id = ? AND user_id = ?
            """;

    try (Connection con = jdbcconnection.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setBoolean(1, read);
        ps.setBoolean(2, read);
        ps.setLong(3, id);
        ps.setLong(4, userId);

        return ps.executeUpdate() > 0;

    } catch (Exception e) {
        System.out.println("Notification update error: " + e.getMessage());
        return false;
    }
}

public boolean deleteById(Long id, Long userId) {
    String sql = """
            DELETE FROM notifications
            WHERE notification_id = ? AND user_id = ?
            """;

    try (Connection con = jdbcconnection.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setLong(1, id);
        ps.setLong(2, userId);

        return ps.executeUpdate() > 0;

    } catch (Exception e) {
        System.out.println("Notification deletion error: " + e.getMessage());
        return false;
    }
}

}
