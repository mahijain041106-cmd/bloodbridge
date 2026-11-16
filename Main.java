package backend;
import java.util.*;
public class Main {

    public static void main(String[] args) {

        AuthManager auth = new AuthManager();
        Scanner in = new Scanner(System.in);

        // Create the first administrator when requested
        if (args.length > 0 && args[0].equals("--seed-admin")) {
            try {
                auth.createAdmin(
                    "System Admin",
                    "admin",
                    "admin@bloodbridge.local",
                    "Admin@123"
                );

                System.out.println("Admin account created successfully.");
                System.out.println("Username: admin");
                System.out.println("Change the default password after login.");

            } catch (RuntimeException e) {
                System.out.println("Admin creation failed: " + e.getMessage());
            }

            in.close();
            return;
        }

        System.out.println("================================");
        System.out.println("       BLOODBRIDGE SYSTEM");
        System.out.println(" Educational Allocation Prototype");
        System.out.println("================================");

        boolean running = true;

        while (running) {
            try {

                if (!auth.isLoggedIn()) {
                    running = loggedOutMenu(auth, in);
                } else {
                    loggedInMenu(auth, in);
                }

            } catch (RuntimeException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }

        System.out.println("Thank you for using BloodBridge.");
        in.close();
    }

    // Menu shown when the user is not logged in
    private static boolean loggedOutMenu(AuthManager auth, Scanner in) {

        System.out.println("\n========== MAIN MENU ==========");
        System.out.println("1. Register");
        System.out.println("2. Login");
        System.out.println("3. Forgot Password");
        System.out.println("4. Reset Password Using Token");
        System.out.println("0. Exit");

        System.out.print("Enter your choice: ");
        String choice = in.nextLine().trim();

        switch (choice) {

            case "1": {
                System.out.print("Full Name: ");
                String name = in.nextLine().trim();

                System.out.print("Username: ");
                String username = in.nextLine().trim();

                System.out.print("Email: ");
                String email = in.nextLine().trim();

                System.out.print("Password: ");
                String password = in.nextLine();

                System.out.print("Role (REQUESTER / INVENTORY_OPERATOR): ");
                String role = in.nextLine().trim().toUpperCase();

                auth.register(name, username, email, password, role);

                System.out.println("Registration successful.");
                break;
            }

            case "2": {
                System.out.print("Username: ");
                String username = in.nextLine().trim();

                System.out.print("Password: ");
                String password = in.nextLine();

                User user = auth.login(username, password);

                System.out.println("Login successful. Welcome!");
                System.out.println("Logged in as: " + user);

                break;
            }

            case "3": {
                System.out.print("Enter registered email: ");
                String email = in.nextLine().trim();

                String token = auth.requestPasswordReset(email);

                System.out.println(
                    "If the account exists, a reset token has been generated."
                );

                if (token != null) {
                    System.out.println("[DEMO RESET TOKEN]: " + token);
                }

                break;
            }

            case "4": {
                System.out.print("Enter reset token: ");
                String token = in.nextLine().trim();

                System.out.print("Enter new password: ");
                String newPassword = in.nextLine();

                auth.resetPassword(token, newPassword);

                System.out.println("Password reset successfully.");
                break;
            }

            case "0":
                return false;

            default:
                System.out.println("Invalid choice. Please try again.");
        }

        return true;
    }

    // Menu shown after successful login
    private static void loggedInMenu(AuthManager auth, Scanner in) {

        boolean admin = auth.hasRole("ADMIN");

        System.out.println("\n========== USER MENU ==========");
        System.out.println("1. Change Password");
        System.out.println("2. Logout");

        if (admin) {
            System.out.println("3. Deactivate User");
            System.out.println("4. Activate User");
            System.out.println("5. View Audit Log");
        } else {
            System.out.println("5. View My Activity");
        }

        System.out.print("Enter your choice: ");
        String choice = in.nextLine().trim();

        switch (choice) {

            case "1": {
                System.out.print("Current Password: ");
                String currentPassword = in.nextLine();

                System.out.print("New Password: ");
                String newPassword = in.nextLine();

                auth.changePassword(currentPassword, newPassword);

                System.out.println("Password changed successfully.");
                break;
            }

            case "2":
                auth.logout();
                System.out.println("Logged out successfully.");
                break;

            case "3":
                if (admin) {
                    System.out.print("Username to deactivate: ");
                    String username = in.nextLine().trim();

                    auth.deactivateUser(username);
                    System.out.println("Account deactivated.");
                } else {
                    System.out.println("Access denied.");
                }
                break;

            case "4":
                if (admin) {
                    System.out.print("Username to activate: ");
                    String username = in.nextLine().trim();

                    auth.activateUser(username);
                    System.out.println("Account activated.");
                } else {
                    System.out.println("Access denied.");
                }
                break;

            case "5": {
                AuditManager audit = new AuditManager();

                for (AuditEntry entry :
                        admin
                        ? audit.getRecent(auth, 20)
                        : audit.getMyHistory(auth, 20)) {

                    System.out.println(entry);
                }

                break;
            }

            default:
                System.out.println("Invalid choice. Please try again.");
        }
    }
}
