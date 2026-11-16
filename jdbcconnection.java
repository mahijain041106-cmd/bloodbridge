package backend;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class jdbcconnection {

    private static final String URL =
            "jdbc:mysql://localhost:3306/bloodbridge_db";

    private static final String USER = "root";

    private static final String PASSWORD = "Mahijain@041106";

    public static Connection getConnection() throws SQLException {

        return DriverManager.getConnection(URL, USER, PASSWORD);

    }
}
