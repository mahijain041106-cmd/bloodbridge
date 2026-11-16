
package backend;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class backend {

    private static final AuthManager auth = new AuthManager();
    private static final RequestService requestService = new RequestService();
    private static final NotificationController notificationController =
            new NotificationController();
    private static final InventoryController inventoryController =
            new InventoryController();

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        try {
            while (true) {

                System.out.println("\n================================");
                System.out.println("       BLOODBRIDGE SYSTEM");
                System.out.println("================================");
                System.out.println("1. Login");
                System.out.println("2. Register");
                System.out.println("3. Exit");
                System.out.print("\nEnter choice: ");

                String choice = sc.nextLine();

                switch (choice) {
                    case "1":
                        login(sc);
                        break;

                    case "2":
                        register(sc);
                        break;

                    case "3":
                        System.out.println("Thank you for using BloodBridge.");
                        return;

                    default:
                        System.out.println("Invalid choice.");
                }
            }
        } finally {
            sc.close();
        }
    }

    private static void login(Scanner sc) {
        try {
            System.out.print("\nEnter Username: ");
            String username = sc.nextLine();

            System.out.print("Enter Password: ");
            String password = sc.nextLine();

            User user = auth.login(username, password);

            System.out.println("\nLogin successful!");
            System.out.println("Welcome, " + user.getFullName());
            System.out.println("Role: " + user.getRole());

            userMenu(sc);

        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("Login failed: " + e.getMessage());
        }
    }

    private static void register(Scanner sc) {
        try {
            System.out.println("\n===== USER REGISTRATION =====");

            System.out.print("Enter Full Name: ");
            String fullName = sc.nextLine();

            System.out.print("Enter Username: ");
            String username = sc.nextLine();

            System.out.print("Enter Email: ");
            String email = sc.nextLine();

            System.out.print("Enter Password: ");
            String password = sc.nextLine();

            System.out.println("\nAvailable Roles:");
            System.out.println("1. REQUESTER");
            System.out.println("2. INVENTORY_OPERATOR");
            System.out.print("Choose Role: ");

            String roleChoice = sc.nextLine();

            String roleName;

            if (roleChoice.equals("1")) {
                roleName = "REQUESTER";
            } else if (roleChoice.equals("2")) {
                roleName = "INVENTORY_OPERATOR";
            } else {
                System.out.println("Invalid role choice.");
                return;
            }

            auth.register(fullName, username, email, password, roleName);

            System.out.println("\nRegistration successful!");
            System.out.println("You can now log in.");

        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("Registration failed: " + e.getMessage());
        }
    }

    // Select dashboard according to logged-in user's role
    private static void userMenu(Scanner sc) {

        while (auth.isLoggedIn()) {

            User user = auth.getCurrentUser();

            if (user == null || !user.isActive()) {
                System.out.println("Account is inactive.");
                auth.logout();
                return;
            }

            if (auth.hasRole("REQUESTER")) {
                requesterMenu(sc, user);
            } else if (auth.hasRole("INVENTORY_OPERATOR")) {
                inventoryMenu(sc, user);
            } else {
                System.out.println("Access denied.");
                auth.logout();
            }
        }
    }

    // REQUESTER DASHBOARD
    private static void requesterMenu(Scanner sc, User user) {

        System.out.println("\n================================");
        System.out.println("       REQUESTER DASHBOARD");
        System.out.println("================================");
        System.out.println("Logged in as: " + user.getFullName());
        System.out.println("1. Submit Blood Request");
        System.out.println("2. View Notifications");
        System.out.println("3. My Blood Requests");
        System.out.println("4. Logout");
        System.out.print("Enter choice: ");

        String choice = sc.nextLine();

        switch (choice) {
            case "1":
                submitBloodRequest(sc, user);
                break;

            case "2":
                viewNotifications(sc, user);
                break;

            case "3":
                viewMyRequests(user);
                break;

            case "4":
                auth.logout();
                System.out.println("Logged out successfully.");
                break;

            default:
                System.out.println("Invalid choice.");
        }
    }

    // INVENTORY OPERATOR DASHBOARD
    private static void inventoryMenu(Scanner sc, User user) {

        while (true) {
            System.out.println("\n================================");
            System.out.println("    INVENTORY OPERATOR DASHBOARD");
            System.out.println("================================");
            System.out.println("Logged in as: " + user.getFullName());

            System.out.println("\n1. View All Blood Units");
            System.out.println("2. View Available Units");
            System.out.println("3. View Reserved Units");
            System.out.println("4. View Expired Units");
            System.out.println("5. Add New Blood Unit");
            System.out.println("6. Inventory Summary");
            System.out.println("7. Update Expired Units");
            System.out.println("8. View Notifications");
            System.out.println("9. Search by Blood Group");
            System.out.println("10. Update Blood Unit");
            System.out.println("11. Delete Blood Unit");
            System.out.println("13. Request Summary");
            System.out.println("14. Reservation Summary");
            System.out.println("12. Logout");

            System.out.print("\nEnter choice: ");

            try {
                int choice = Integer.parseInt(sc.nextLine());

                switch (choice) {
                    case 1:
                        displayInventory(inventoryController.getAll());
                        break;

                    case 2:
                        displayInventory(inventoryController.getByStatus("AVAILABLE"));
                        break;

                    case 3:
                        displayInventory(inventoryController.getByStatus("RESERVED"));
                        break;

                    case 4:
                        displayInventory(inventoryController.getByStatus("EXPIRED"));
                        break;

                    case 5:
                        addBloodUnit(sc, user);
                        break;

                    case 6:
                        displayInventorySummary();
                        break;

                    case 7:
                        updateExpiredUnits();
                        break;

                    case 8:
                    	viewNotifications(sc, user);
                        break;

                    case 9:
                        System.out.print("Enter Blood Group: ");
                        displayInventory(
                            inventoryController.getByCategory(sc.nextLine())
                        );
                        break;

                    case 10:
                        updateBloodUnit(sc);
                        break;

                    case 11:
                        deleteBloodUnit(sc);
                        break;

                    case 12:
                        System.out.println("Logging out...");
                        auth.logout();
                        return;

                    case 13:
                        displayRequestSummary();
                        break;

                    case 14:
                        displayReservationSummary();
                        break;
                        
                    default:
                        System.out.println("Invalid choice!");
                }

            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    // DISPLAY INVENTORY
    private static void displayInventory(List<Map<String, Object>> units) {

        System.out.println("\n===== BLOOD INVENTORY =====");

        if (units == null || units.isEmpty()) {
            System.out.println("No blood units found.");
            return;
        }

        for (Map<String, Object> unit : units) {

            System.out.println("\n--------------------------------");
            for (Map.Entry<String, Object> entry : unit.entrySet()) {
                System.out.println(entry.getKey() + ": " + entry.getValue());
            }
        }

        System.out.println("--------------------------------");
    }

    // ADD NEW BLOOD UNIT
    private static void addBloodUnit(Scanner sc, User user) {

        try {
            System.out.println("\n===== ADD NEW BLOOD UNIT =====");

            System.out.print("Enter Unit Code: ");
            String unitCode = sc.nextLine();

            System.out.println("Available Blood Groups:");
            System.out.println("A+, A-, B+, B-, AB+, AB-, O+, O-");

            System.out.print("Enter Blood Group: ");
            String bloodGroup = sc.nextLine();

            System.out.println("\nComponents:");
            System.out.println("1. Whole Blood");
            System.out.println("2. Red Blood Cells");
            System.out.println("3. Plasma");
            System.out.println("4. Platelets");

            System.out.print("Enter Component ID: ");
            int componentId = Integer.parseInt(sc.nextLine());

            System.out.print("Enter Collection Date (YYYY-MM-DD): ");
            LocalDate collectionDate =
                    LocalDate.parse(sc.nextLine());

            System.out.print("Enter Expiry Date (YYYY-MM-DD): ");
            LocalDate expiryDate =
                    LocalDate.parse(sc.nextLine());

            Map<String, Object> response = inventoryController.add(
                    unitCode,
                    bloodGroup,
                    componentId,
                    collectionDate,
                    expiryDate,
                    user.getUserId()
            );

            System.out.println("\n" + response.get("message"));

        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid component ID.");
        } catch (Exception e) {
            System.out.println("Unable to add blood unit: " + e.getMessage());
        }
    }

    // INVENTORY SUMMARY
    private static void displayInventorySummary() {

        System.out.println("\n===== INVENTORY SUMMARY =====");

        try {
            Map<String, Long> counts = inventoryController.count();

            System.out.println("Total Units: " + counts.get("total"));
            System.out.println("Available Units: " + counts.get("available"));
            System.out.println("Reserved Units: " + counts.get("reserved"));
            System.out.println("Expired Units: " + counts.get("expired"));
            System.out.println("Inactive Units: " + counts.get("inactive"));

        } catch (Exception e) {
            System.out.println("Unable to retrieve inventory summary: "
                    + e.getMessage());
        }
    }

    // MARK EXPIRED UNITS
    private static void updateExpiredUnits() {

        try {
            Map<String, Integer> response =
                    inventoryController.markExpired();

            System.out.println("\nExpired inventory updated.");
            System.out.println("Units marked expired: "
                    + response.get("updatedUnits"));

        } catch (Exception e) {
            System.out.println("Unable to update expired units: "
                    + e.getMessage());
        }
    }

    // SUBMIT BLOOD REQUEST
    private static void submitBloodRequest(Scanner sc, User user) {

        try {
            System.out.println("\n===== BLOOD REQUEST FORM =====");

            System.out.println("Available Blood Groups:");
            System.out.println("A+, A-, B+, B-, AB+, AB-, O+, O-");

            System.out.print("Enter Required Blood Group: ");
            String bloodGroup = sc.nextLine();

            System.out.println("\nAvailable Components:");
            System.out.println("1. Whole Blood");
            System.out.println("2. Red Blood Cells");
            System.out.println("3. Plasma");
            System.out.println("4. Platelets");

            System.out.print("Enter Component ID: ");
            int componentId = Integer.parseInt(sc.nextLine());

            System.out.print("Enter Quantity: ");
            int quantity = Integer.parseInt(sc.nextLine());

            System.out.println("\nPriority Levels:");
            System.out.println("1. Low");
            System.out.println("2. Normal");
            System.out.println("3. High");
            System.out.println("4. Urgent");

            System.out.print("Enter Priority: ");
            int priority = Integer.parseInt(sc.nextLine());

            System.out.print("Enter Request Note: ");
            String note = sc.nextLine();

            request newRequest = new request(
                    user.getUserId(),
                    bloodGroup,
                    componentId,
                    quantity,
                    priority,
                    note
            );

            long requestId = requestService.submitRequest(newRequest);

            System.out.println("\n================================");
            System.out.println("REQUEST SUBMITTED SUCCESSFULLY!");
            System.out.println("Request ID: " + requestId);
            System.out.println("Status: SUBMITTED");
            System.out.println("Validation: VALID");
            System.out.println("================================");

        } catch (NumberFormatException e) {
            System.out.println("Please enter valid numeric values.");
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid request: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    // VIEW NOTIFICATIONS
    private static void viewNotifications(Scanner sc, User user) {

        System.out.println("\n===== YOUR NOTIFICATIONS =====");

        try {
            List<NotificationRecord> notifications =
                    notificationController.getUserNotifications(
                            (long) user.getUserId()
                    );

            if (notifications == null || notifications.isEmpty()) {
                System.out.println("No notifications found.");
                return;
            }

            for (NotificationRecord notification : notifications) {

                System.out.println("\n--------------------------------");
                System.out.println("Notification ID: "
                        + notification.getNotificationId());
                System.out.println("Title: " + notification.getTitle());
                System.out.println("Type: "
                        + notification.getNotificationType());
                System.out.println("Message: " + notification.getMessage());
                System.out.println("Request ID: "
                        + notification.getRequestId());
                System.out.println("Status: "
                        + (notification.isRead() ? "READ" : "UNREAD"));
                System.out.println("Created At: "
                        + notification.getCreatedAt());
            }

            System.out.print(
                    "\nEnter Notification ID to mark as read (0 to skip): ");

            long notificationId = Long.parseLong(sc.nextLine());

            if (notificationId != 0) {

                boolean updated =
                        notificationController.markAsRead(notificationId, true);

                System.out.println(updated
                        ? "Notification marked as read."
                        : "Could not update notification.");
            }

        } catch (NumberFormatException e) {
            System.out.println("Invalid notification ID.");
        } catch (Exception e) {
            System.out.println("Notification error: " + e.getMessage());
        }
    }

    // VIEW MY REQUESTS
    private static void viewMyRequests(User user) {

        System.out.println("\n===== MY BLOOD REQUESTS =====");

        try {
            List<RequestRecord> requests =
                    requestService.getUserRequests(user.getUserId());

            if (requests == null || requests.isEmpty()) {
                System.out.println("You have not submitted any requests.");
                return;
            }

            for (RequestRecord record : requests) {

                System.out.println("\n--------------------------------");
                System.out.println("Request ID: " + record.getRequestId());
                System.out.println("Request Code: " + record.getRequestCode());
                System.out.println("Blood Group: " + record.getBloodGroup());
                System.out.println("Component ID: " + record.getComponentId());
                System.out.println("Quantity: " + record.getQuantity());
                System.out.println("Priority: " + record.getPriority());
                System.out.println("Validation: " + record.getValidationStatus());
                System.out.println("Current Status: " + record.getRequestStatus());
                System.out.println("Note: " + record.getRequestNote());
                System.out.println("Submitted At: " + record.getSubmittedAt());
            }

            System.out.println("\n--------------------------------");

        } catch (Exception e) {
            System.out.println("Unable to retrieve requests: " + e.getMessage());
        }
    }
    private static void updateBloodUnit(Scanner sc) {
        try {
            System.out.println("\n===== UPDATE BLOOD UNIT =====");

            System.out.print("Enter Unit ID: ");
            long id = Long.parseLong(sc.nextLine());

            Map<String, Object> existing =
                    inventoryController.getById(id);

            if (existing == null) {
                System.out.println("Blood unit not found.");
                return;
            }

            System.out.println("Current Details: " + existing);

            System.out.print("Enter New Unit Code: ");
            String code = sc.nextLine();

            System.out.print("Enter New Blood Group: ");
            String group = sc.nextLine();

            System.out.print("Enter Component ID: ");
            int component = Integer.parseInt(sc.nextLine());

            System.out.print("Enter Collection Date (YYYY-MM-DD): ");
            LocalDate collection = LocalDate.parse(sc.nextLine());

            System.out.print("Enter Expiry Date (YYYY-MM-DD): ");
            LocalDate expiry = LocalDate.parse(sc.nextLine());

            Map<String, Object> result =
                    inventoryController.update(
                            id, code, group, component, collection, expiry);

            System.out.println(result.get("message"));

        } catch (Exception e) {
            System.out.println("Update error: " + e.getMessage());
        }
    }

    private static void deleteBloodUnit(Scanner sc) {
        try {
            System.out.println("\n===== DELETE BLOOD UNIT =====");

            System.out.print("Enter Unit ID: ");
            long id = Long.parseLong(sc.nextLine());

            Map<String, Object> existing =
                    inventoryController.getById(id);

            if (existing == null) {
                System.out.println("Blood unit not found.");
                return;
            }

            System.out.println("Selected Unit: " + existing);

            System.out.print("Confirm deletion? (yes/no): ");
            String confirmation = sc.nextLine();

            if (confirmation.equalsIgnoreCase("yes")) {
                Map<String, String> result =
                        inventoryController.delete(id);

                System.out.println(result.get("message"));
            } else {
                System.out.println("Deletion cancelled.");
            }

        } catch (Exception e) {
            System.out.println("Delete error: " + e.getMessage());
        }
    }
 // REQUEST SUMMARY
    private static void displayRequestSummary() {

        System.out.println("\n===== REQUEST SUMMARY =====");

        String sql = "SELECT * FROM vw_request_summary";

        try (Connection con = jdbcconnection.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData meta = rs.getMetaData();
            int columns = meta.getColumnCount();

            for (int i = 1; i <= columns; i++) {
                System.out.printf("%-25s", meta.getColumnLabel(i));
            }

            System.out.println();

            while (rs.next()) {
                for (int i = 1; i <= columns; i++) {
                    System.out.printf("%-25s", rs.getObject(i));
                }
                System.out.println();
            }

        } catch (Exception e) {
            System.out.println("Unable to retrieve request summary: "
                    + e.getMessage());
        }
    }


    // RESERVATION SUMMARY
    private static void displayReservationSummary() {

        System.out.println("\n===== RESERVATION SUMMARY =====");

        String sql = "SELECT * FROM vw_reservation_summary";

        try (Connection con = jdbcconnection.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData meta = rs.getMetaData();
            int columns = meta.getColumnCount();

            for (int i = 1; i <= columns; i++) {
                System.out.printf("%-25s", meta.getColumnLabel(i));
            }

            System.out.println();

            while (rs.next()) {
                for (int i = 1; i <= columns; i++) {
                    System.out.printf("%-25s", rs.getObject(i));
                }
                System.out.println();
            }

        } catch (Exception e) {
            System.out.println("Unable to retrieve reservation summary: "
                    + e.getMessage());
        }
    }
}
