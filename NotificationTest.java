
package backend;

public class NotificationTest {

    public static void main(String[] args) {

        NotificationController controller =
                new NotificationController();

        NotificationRequest request = new NotificationRequest();

        request.setUserId(1L);
        request.setRequestId(null);
        request.setReservationId(null);
        request.setNotificationType("SYSTEM");
        request.setTitle("BloodBridge Test Notification");
        request.setMessage(
            "Welcome Rahul! Your BloodBridge notification system is working."
        );

        boolean created = controller.create(request);

        if (created) {
            System.out.println("Test notification created successfully!");
        } else {
            System.out.println("Notification creation failed.");
        }
    }
}
