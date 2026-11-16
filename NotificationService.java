
package backend;

import java.util.List;
import java.util.Optional;

public class NotificationService {

    private final NotificationRepository repository;

    public NotificationService() {
        this.repository = new NotificationRepository();
    }

    public boolean create(NotificationRequest input) {

        if (input == null || input.getUserId() == null) {
            System.out.println("A valid user ID is required.");
            return false;
        }

        if (input.getMessage() == null || input.getMessage().isBlank()) {
            System.out.println("Notification message cannot be empty.");
            return false;
        }

        NotificationRecord notification = new NotificationRecord();

        notification.setUserId(input.getUserId());
        notification.setRequestId(input.getRequestId());
        notification.setReservationId(input.getReservationId());
        notification.setNotificationType(
                input.getNotificationType() == null
                        ? "SYSTEM"
                        : input.getNotificationType()
        );
        notification.setTitle(
                input.getTitle() == null ? "BloodBridge Notification" : input.getTitle()
        );
        notification.setMessage(input.getMessage());
        notification.setRead(false);

        return repository.save(notification);
    }

    public List<NotificationRecord> getAll() {
        return repository.findAll();
    }

    public List<NotificationRecord> getForUser(Long userId) {
        return repository.findByUserId(userId);
    }

    public List<NotificationRecord> getReadStatus(Long userId, boolean read) {
        return repository.findByUserIdAndRead(userId, read);
    }

    public boolean markRead(Long id, boolean read) {
        Optional<NotificationRecord> notification = repository.findById(id);

        if (notification.isEmpty()) {
            System.out.println("Notification not found.");
            return false;
        }

        return repository.markRead(id, read);
    }

    public boolean delete(Long id) {
        if (repository.findById(id).isEmpty()) {
            System.out.println("Notification not found.");
            return false;
        }

        return repository.deleteById(id);
    }
	
	public boolean markRead(Long id, Long userId, boolean read) {
	    if (id == null || userId == null) {
	        return false;
	    }
	
	    return repository.markRead(id, userId, read);
	}
	
	public boolean delete(Long id, Long userId) {
	    if (id == null || userId == null) {
	        return false;
	    }
	
	    return repository.deleteById(id, userId);
	}

}
