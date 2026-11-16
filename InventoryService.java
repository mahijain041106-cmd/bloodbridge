
package backend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InventoryService {

    // Add a new blood unit
    public boolean add(
            String unitCode,
            String bloodGroupCode,
            int componentId,
            LocalDate collectionDate,
            LocalDate expiryDate,
            long createdByUserId) {

        String sql = """
                INSERT INTO blood_units
                (unit_code, blood_group_code, component_id,
                 collection_date, expiry_date, unit_status,
                 created_by_user_id)
                VALUES (?, ?, ?, ?, ?, 'AVAILABLE', ?)
                """;

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, unitCode.trim());
            ps.setString(2, bloodGroupCode.trim());
            ps.setInt(3, componentId);
            ps.setDate(4, java.sql.Date.valueOf(collectionDate));
            ps.setDate(5, java.sql.Date.valueOf(expiryDate));
            ps.setLong(6, createdByUserId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Inventory add error: " + e.getMessage());
            return false;
        }
    }

    // Get all blood units
    public List<Map<String, Object>> getAll() {
        String sql = """
                SELECT bu.*, bg.display_name, c.component_name
                FROM blood_units bu
                JOIN blood_groups bg
                    ON bu.blood_group_code = bg.blood_group_code
                JOIN components c
                    ON bu.component_id = c.component_id
                ORDER BY bu.unit_id
                """;

        return executeQuery(sql);
    }

    // Get a blood unit by ID
    public Map<String, Object> getById(long id) {
        String sql = """
                SELECT bu.*, bg.display_name, c.component_name
                FROM blood_units bu
                JOIN blood_groups bg
                    ON bu.blood_group_code = bg.blood_group_code
                JOIN components c
                    ON bu.component_id = c.component_id
                WHERE bu.unit_id = ?
                """;

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            System.out.println("Inventory search error: " + e.getMessage());
        }

        return null;
    }

    // Get units by status
    public List<Map<String, Object>> getByStatus(String status) {
        if (!isValidStatus(status)) {
            throw new IllegalArgumentException("Invalid blood unit status.");
        }

        String sql = """
                SELECT bu.*, bg.display_name, c.component_name
                FROM blood_units bu
                JOIN blood_groups bg
                    ON bu.blood_group_code = bg.blood_group_code
                JOIN components c
                    ON bu.component_id = c.component_id
                WHERE bu.unit_status = ?
                ORDER BY bu.expiry_date
                """;

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, status.toUpperCase());

            try (ResultSet rs = ps.executeQuery()) {
                return collectRows(rs);
            }

        } catch (SQLException e) {
            System.out.println("Inventory status error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // Get units by blood group
    public List<Map<String, Object>> getByBloodGroup(String bloodGroupCode) {
        String sql = """
                SELECT bu.*, bg.display_name, c.component_name
                FROM blood_units bu
                JOIN blood_groups bg
                    ON bu.blood_group_code = bg.blood_group_code
                JOIN components c
                    ON bu.component_id = c.component_id
                WHERE bu.blood_group_code = ?
                ORDER BY bu.expiry_date
                """;

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, bloodGroupCode.trim());

            try (ResultSet rs = ps.executeQuery()) {
                return collectRows(rs);
            }

        } catch (SQLException e) {
            System.out.println("Inventory group search error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // Update unit details without changing its reservation status
    public boolean update(
            long unitId,
            String unitCode,
            String bloodGroupCode,
            int componentId,
            LocalDate collectionDate,
            LocalDate expiryDate) {

        String sql = """
                UPDATE blood_units
                SET unit_code = ?,
                    blood_group_code = ?,
                    component_id = ?,
                    collection_date = ?,
                    expiry_date = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE unit_id = ?
                AND unit_status <> 'RESERVED'
                """;

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, unitCode.trim());
            ps.setString(2, bloodGroupCode.trim());
            ps.setInt(3, componentId);
            ps.setDate(4, java.sql.Date.valueOf(collectionDate));
            ps.setDate(5, java.sql.Date.valueOf(expiryDate));
            ps.setLong(6, unitId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Inventory update error: " + e.getMessage());
            return false;
        }
    }

    // Delete a unit only if it is not reserved

 // Safely remove a unit from active inventory
 public boolean delete(long unitId) {
     String sql = """
             UPDATE blood_units
             SET unit_status = 'INACTIVE',
                 updated_at = CURRENT_TIMESTAMP
             WHERE unit_id = ?
             AND unit_status IN ('AVAILABLE', 'EXPIRED')
             """;

     try (Connection con = jdbcconnection.getConnection();
          PreparedStatement ps = con.prepareStatement(sql)) {

         ps.setLong(1, unitId);
         return ps.executeUpdate() > 0;

     } catch (SQLException e) {
         System.out.println("Inventory delete error: " + e.getMessage());
         return false;
     }
 }


    // Count all blood units
    public long countAll() {
        return count("SELECT COUNT(*) FROM blood_units");
    }

    // Count units by status
    public long countByStatus(String status) {
        if (!isValidStatus(status)) {
            throw new IllegalArgumentException("Invalid blood unit status.");
        }

        String sql = "SELECT COUNT(*) FROM blood_units WHERE unit_status = ?";

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, status.toUpperCase());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }

        } catch (SQLException e) {
            System.out.println("Inventory count error: " + e.getMessage());
        }

        return 0;
    }

    // Mark available units as expired when their expiry date has passed
    public int markExpiredUnits() {
        String sql = """
                UPDATE blood_units
                SET unit_status = 'EXPIRED',
                    updated_at = CURRENT_TIMESTAMP
                WHERE unit_status = 'AVAILABLE'
                AND expiry_date < CURDATE()
                """;

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            return ps.executeUpdate();

        } catch (SQLException e) {
            System.out.println("Expiry update error: " + e.getMessage());
            return 0;
        }
    }

    // Helper: execute a SELECT query
    private List<Map<String, Object>> executeQuery(String sql) {
        try (Connection con = jdbcconnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            return collectRows(rs);

        } catch (SQLException e) {
            System.out.println("Inventory query error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // Helper: convert database rows into maps
    private List<Map<String, Object>> collectRows(ResultSet rs)
            throws SQLException {

        List<Map<String, Object>> rows = new ArrayList<>();

        while (rs.next()) {
            rows.add(mapRow(rs));
        }

        return rows;
    }

    private Map<String, Object> mapRow(ResultSet rs)
            throws SQLException {

        Map<String, Object> row = new LinkedHashMap<>();

        row.put("unitId", rs.getLong("unit_id"));
        row.put("unitCode", rs.getString("unit_code"));
        row.put("bloodGroupCode", rs.getString("blood_group_code"));
        row.put("bloodGroupName", rs.getString("display_name"));
        row.put("componentId", rs.getInt("component_id"));
        row.put("componentName", rs.getString("component_name"));
        row.put("collectionDate", rs.getDate("collection_date"));
        row.put("expiryDate", rs.getDate("expiry_date"));
        row.put("status", rs.getString("unit_status"));

        return row;
    }

    private long count(String sql) {
        try (Connection con = jdbcconnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getLong(1);
            }

        } catch (SQLException e) {
            System.out.println("Inventory count error: " + e.getMessage());
        }

        return 0;
    }

    private boolean isValidStatus(String status) {
        return status != null &&
                List.of("AVAILABLE", "RESERVED", "EXPIRED", "INACTIVE")
                        .contains(status.toUpperCase());
    }
}
