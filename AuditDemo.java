
package backend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AuditDemo {

    public static void main(String[] args) throws Exception {

        String password = args.length > 0
                ? args[0]
                : "Admin@123";

        AuthManager auth = new AuthManager();

        try {
            auth.login("admin", "wrong-password-1");
        } catch (RuntimeException e) {
            System.out.println("Expected failure: " + e.getMessage());
        }

        auth.login("admin", password);

        AuditManager audit = new AuditManager();

        System.out.println("\nLast 10 audit records:");

        for (AuditEntry entry : audit.getRecent(auth, 10)) {
            System.out.println("  " + entry);
        }

        System.out.println(
                "\nTrying to modify the newest audit record:");

        try (Connection con = jdbcconnection.getConnection()) {

            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE audit_logs SET details = 'tampered' "
                    + "ORDER BY audit_id DESC LIMIT 1")) {

                ps.executeUpdate();
                System.out.println(
                        "UPDATE was not blocked. Check audit protection triggers.");

            } catch (SQLException e) {
                System.out.println("UPDATE blocked: " + e.getMessage());
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "DELETE FROM audit_logs "
                    + "ORDER BY audit_id DESC LIMIT 1")) {

                ps.executeUpdate();
                System.out.println(
                        "DELETE was not blocked. Check audit protection triggers.");

            } catch (SQLException e) {
                System.out.println("DELETE blocked: " + e.getMessage());
            }
        }

        auth.logout();

        System.out.println(
                "\nLogged out. This should create a USER_LOGOUT record.");
    }
}
