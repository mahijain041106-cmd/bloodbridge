
package backend;

public class DashboardTest {

    public static void main(String[] args) {

        DashboardDAO dashboard = new DashboardDAO();

        dashboard.displayInventorySummary();

        dashboard.displayInventoryStatistics();

        dashboard.displayRequestSummary();

        dashboard.displayReservationSummary();
    }
}
