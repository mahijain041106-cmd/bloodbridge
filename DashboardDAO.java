
package backend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DashboardDAO {

    // Display inventory summary
    public void displayInventorySummary() {

        String sql =
            "SELECT blood_group, component_name, unit_status, unit_count " +
            "FROM vw_inventory_summary " +
            "ORDER BY blood_group, component_name, unit_status";

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.println("\n========== INVENTORY DASHBOARD ==========");

            while (rs.next()) {

                System.out.println(
                    "Blood Group: " + rs.getString("blood_group") +
                    " | Component: " + rs.getString("component_name") +
                    " | Status: " + rs.getString("unit_status") +
                    " | Count: " + rs.getLong("unit_count")
                );
            }

            System.out.println("==========================================");

        } catch (SQLException e) {
            System.out.println(
                "Inventory dashboard error: " + e.getMessage()
            );
        }
    }

    // Display overall inventory statistics
    public void displayInventoryStatistics() {

        String sql =
            "SELECT " +
            "COUNT(*) AS total_units, " +
            "SUM(CASE WHEN unit_status = 'AVAILABLE' THEN 1 ELSE 0 END) AS available_units, " +
            "SUM(CASE WHEN unit_status = 'RESERVED' THEN 1 ELSE 0 END) AS reserved_units, " +
            "SUM(CASE WHEN unit_status = 'EXPIRED' THEN 1 ELSE 0 END) AS expired_units " +
            "FROM blood_units";

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {

                System.out.println("\n========== INVENTORY STATISTICS ==========");

                System.out.println("Total Units: " +
                    rs.getLong("total_units"));

                System.out.println("Available Units: " +
                    rs.getLong("available_units"));

                System.out.println("Reserved Units: " +
                    rs.getLong("reserved_units"));

                System.out.println("Expired Units: " +
                    rs.getLong("expired_units"));

                System.out.println("==========================================");
            }

        } catch (SQLException e) {
            System.out.println(
                "Statistics error: " + e.getMessage()
            );
        }
    }

    // Display request summary
    public void displayRequestSummary() {

        String sql =
            "SELECT request_status, priority_level, request_count " +
            "FROM vw_request_summary " +
            "ORDER BY priority_level, request_status";

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.println("\n========== REQUEST DASHBOARD ==========");

            while (rs.next()) {

                System.out.println(
                    "Status: " + rs.getString("request_status") +
                    " | Priority: " + rs.getInt("priority_level") +
                    " | Count: " + rs.getLong("request_count")
                );
            }

            System.out.println("========================================");

        } catch (SQLException e) {
            System.out.println(
                "Request dashboard error: " + e.getMessage()
            );
        }
    }

    // Display reservation summary
    public void displayReservationSummary() {

        String sql =
            "SELECT reservation_status, reservation_count " +
            "FROM vw_reservation_summary " +
            "ORDER BY reservation_status";

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.println("\n========== RESERVATION DASHBOARD ==========");

            while (rs.next()) {

                System.out.println(
                    "Status: " + rs.getString("reservation_status") +
                    " | Count: " + rs.getLong("reservation_count")
                );
            }

            System.out.println("============================================");

        } catch (SQLException e) {
            System.out.println(
                "Reservation dashboard error: " + e.getMessage()
            );
        }
    }
}
