
package backend;

import java.sql.SQLException;

public class RequestService {

    private final requestDAO dao = new requestDAO();

    private final NotificationController notificationController =
            new NotificationController();

    public long submitRequest(request req) throws SQLException {

        // 1. Validate requester ID
        if (req.getRequesterUserId() <= 0) {
            throw new IllegalArgumentException(
                    "Invalid requester ID!");
        }

        // 2. Validate blood group
        if (req.getRequiredGroupCode() == null ||
                req.getRequiredGroupCode().trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Blood group is required!");
        }

        // 3. Validate component ID
        if (req.getComponentId() <= 0) {
            throw new IllegalArgumentException(
                    "Invalid component ID!");
        }

        // 4. Validate quantity
        if (req.getQuantity() <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than zero!");
        }

        // 5. Validate priority
        if (req.getPriorityLevel() < 1 ||
                req.getPriorityLevel() > 4) {
            throw new IllegalArgumentException(
                    "Priority must be between 1 and 4!");
        }

        // 6. Validate request note
        if (req.getRequestNote() != null &&
                req.getRequestNote().length() > 500) {
            throw new IllegalArgumentException(
                    "Request note cannot exceed 500 characters!");
        }

        // 7. Check requester in database
        if (!dao.userExists(req.getRequesterUserId())) {
            throw new IllegalArgumentException(
                    "User does not exist or is inactive!");
        }

        // 8. Check blood group in database
        String groupCode = req.getRequiredGroupCode().trim();

        if (!dao.bloodGroupExists(groupCode)) {
            throw new IllegalArgumentException(
                    "Invalid blood group!");
        }

        // 9. Check component in database
        if (!dao.componentExists(req.getComponentId())) {
            throw new IllegalArgumentException(
                    "Component does not exist or is inactive!");
        }

        // 10. Insert validated request
        long requestId = dao.insertRequest(req);

        // 11. Create request submission notification
        if (requestId > 0) {

            NotificationRequest notification =
                    new NotificationRequest();

            notification.setUserId(
                    Long.valueOf(req.getRequesterUserId()));

            notification.setRequestId(
                    Long.valueOf(requestId));

            notification.setReservationId(null);

            notification.setNotificationType(
                    "REQUEST_SUBMITTED");

            notification.setTitle(
                    "Blood Request Submitted");

            notification.setMessage(
                    "Your blood request has been submitted successfully. " +
                    "Request ID: " + requestId +
                    ". You will receive updates about its status.");

            boolean notificationCreated =
                    notificationController.create(notification);

            if (!notificationCreated) {
                System.out.println(
                        "Request saved, but notification could not be created.");
            }
        }

        return requestId;
    }
    public java.util.List<RequestRecord> getUserRequests(int userId)
            throws SQLException {

        if (userId <= 0) {
            throw new IllegalArgumentException("Invalid user ID!");
        }

        return dao.getRequestsByUser(userId);
    }
 // Retrieve all blood requests for admin
    public java.util.List<RequestRecord> getAllRequests()
            throws SQLException {

        return dao.getAllRequests();
    }
}
