
package backend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BloodUnitRepository {

    public List<BloodUnit> findAll() {
        String sql = """
                SELECT * FROM blood_units
                ORDER BY unit_id
                """;

        List<BloodUnit> units = new ArrayList<>();

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                units.add(mapUnit(rs));
            }

        } catch (Exception e) {
            System.out.println("Blood unit retrieval error: " + e.getMessage());
        }

        return units;
    }

    public Optional<BloodUnit> findById(long id) {
        String sql = """
                SELECT * FROM blood_units
                WHERE unit_id = ?
                """;

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapUnit(rs));
                }
            }

        } catch (Exception e) {
            System.out.println("Blood unit search error: " + e.getMessage());
        }

        return Optional.empty();
    }

    public Optional<BloodUnit> findByUnitCode(String unitCode) {
        String sql = """
                SELECT * FROM blood_units
                WHERE unit_code = ?
                """;

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, unitCode);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapUnit(rs));
                }
            }

        } catch (Exception e) {
            System.out.println("Unit code search error: " + e.getMessage());
        }

        return Optional.empty();
    }

    public boolean existsByUnitCode(String unitCode) {
        String sql = """
                SELECT 1 FROM blood_units
                WHERE unit_code = ?
                LIMIT 1
                """;

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, unitCode);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (Exception e) {
            System.out.println("Unit existence check error: " + e.getMessage());
            return false;
        }
    }

    public List<BloodUnit> findByStatus(UnitStatus status) {
        String sql = """
                SELECT * FROM blood_units
                WHERE unit_status = ?
                ORDER BY expiry_date
                """;

        List<BloodUnit> units = new ArrayList<>();

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, status.name());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    units.add(mapUnit(rs));
                }
            }

        } catch (Exception e) {
            System.out.println("Status search error: " + e.getMessage());
        }

        return units;
    }

    public List<BloodUnit> findByBloodGroupCode(String bloodGroupCode) {
        String sql = """
                SELECT * FROM blood_units
                WHERE blood_group_code = ?
                ORDER BY expiry_date
                """;

        List<BloodUnit> units = new ArrayList<>();

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, bloodGroupCode);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    units.add(mapUnit(rs));
                }
            }

        } catch (Exception e) {
            System.out.println("Blood group search error: " + e.getMessage());
        }

        return units;
    }

    private BloodUnit mapUnit(ResultSet rs) throws Exception {
        long createdBy = rs.getLong("created_by_user_id");
        Long createdByUserId = rs.wasNull() ? null : createdBy;

        return new BloodUnit(
                rs.getLong("unit_id"),
                rs.getString("unit_code"),
                rs.getString("blood_group_code"),
                rs.getInt("component_id"),
                rs.getDate("collection_date").toLocalDate(),
                rs.getDate("expiry_date").toLocalDate(),
                Enum.valueOf(UnitStatus.class, rs.getString("unit_status")),
                createdByUserId,
                rs.getTimestamp("created_at"),
                rs.getTimestamp("updated_at")
        );
    }
}
