
package backend;

public class NotificationRequest {

    private Long userId;
    private Long requestId;
    private Long reservationId;
    private String notificationType;
    private String title;
    private String message;

    public NotificationRequest() {
    }

    public NotificationRequest(Long userId, Long requestId,
            Long reservationId, String notificationType,
            String title, String message) {

        this.userId = userId;
        this.requestId = requestId;
        this.reservationId = reservationId;
        this.notificationType = notificationType;
        this.title = title;
        this.message = message;
    }

    public Long userId() {
        return userId;
    }

    public Long requestId() {
        return requestId;
    }

    public Long reservationId() {
        return reservationId;
    }

    public String notificationType() {
        return notificationType;
    }

    public String title() {
        return title;
    }

    public String message() {
        return message;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
