package backend;

import java.util.List;

public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController() {
        this.notificationService = new NotificationService();
    }

    public boolean create(NotificationRequest request) {
        return notificationService.create(request);
    }

    public List<NotificationRecord> getAllNotifications() {
        return notificationService.getAll();
    }

    public List<NotificationRecord> getUserNotifications(Long userId) {
        return notificationService.getForUser(userId);
    }

    public List<NotificationRecord> getNotificationsByReadStatus(
            Long userId, boolean isRead) {

        return notificationService.getReadStatus(userId, isRead);
    }

    public boolean markAsRead(Long notificationId, boolean isRead) {
        return notificationService.markRead(notificationId, isRead);
    }

    public boolean deleteNotification(Long notificationId) {
        return notificationService.delete(notificationId);
    }

	public boolean markAsRead(
	        Long notificationId, Long userId, boolean isRead) {
	
	    return notificationService.markRead(
	            notificationId, userId, isRead
	    );
	}
	
	public boolean deleteNotification(Long notificationId, Long userId) {
	    return notificationService.delete(notificationId, userId);
	}

}