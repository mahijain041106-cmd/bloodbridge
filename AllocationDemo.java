
package backend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AllocationDemo {

    public static void main(String[] args) {

        AllocationEngine engine = new AllocationEngine();

        System.out.println("==================================");
        System.out.println(" BLOODBRIDGE ALLOCATION DEMO");
        System.out.println("==================================");

        if (args.length > 0) {
            try {
                int requestId = Integer.parseInt(args[0]);
                show(engine.allocate(requestId));
            } catch (NumberFormatException e) {
                System.out.println("Please provide a valid request ID.");
            } catch (RuntimeException e) {
                System.out.println("Allocation error: " + e.getMessage());
            }

            return;
        }

        String sql =
                "SELECT r.request_id, r.required_group_code, "
                + "c.component_name, r.quantity, r.request_status "
                + "FROM requests r "
                + "JOIN components c ON c.component_id = r.component_id "
                + "ORDER BY r.priority_level DESC, r.created_at ASC "
                + "LIMIT 10";

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            boolean found = false;

            while (rs.next()) {
                found = true;

                int requestId = rs.getInt("request_id");

                System.out.println("\n----------------------------------");
                System.out.println("Request ID: " + requestId);
                System.out.println("Required group: "
                        + rs.getString("required_group_code"));
                System.out.println("Component: "
                        + rs.getString("component_name"));
                System.out.println("Quantity: "
                        + rs.getInt("quantity"));
                System.out.println("Status: "
                        + rs.getString("request_status"));

                try {
                    AllocationResult result =
                            engine.allocate(requestId);

                    show(result);

                } catch (RuntimeException e) {
                    System.out.println(
                            "Could not allocate: " + e.getMessage());
                }
            }

            if (!found) {
                System.out.println(
                        "\nNo requests found. Submit a request first.");
            }

        } catch (Exception e) {
            System.out.println(
                    "\nDatabase error: " + e.getMessage());
        }

        System.out.println("\n==================================");
        System.out.println(" ALLOCATION DEMO FINISHED");
        System.out.println("==================================");
    }

    private static void show(AllocationResult result) {

        System.out.println("\nAllocation result:");
        System.out.println("Recipient: "
                + result.getRecipientGroup());
        System.out.println("Component: "
                + result.getComponent());
        System.out.println("Required quantity: "
                + result.getQuantity());
        System.out.println("Rule version: "
                + result.getRuleVersionName());
        System.out.println("Outcome: "
                + result.getOutcome());

        if (result.getCandidates().isEmpty()) {
            System.out.println("No compatible available units found.");
            return;
        }

        System.out.println("\nCompatible units:");

        int count = 0;

        for (MatchCandidate candidate : result.getCandidates()) {
            String marker = count < result.getQuantity()
                    ? "* " : "  ";

            System.out.println(marker + candidate);
            count++;
        }

        System.out.println(
                "\n* indicates a unit that would be selected.");
        System.out.println(
                "No inventory unit is reserved by this demo.");
    }
}
