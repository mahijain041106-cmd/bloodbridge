
package backend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

public class RecoveryBloodUnitRepository {

    public Optional<RecoveryBloodUnit> findById(Long unitId) {
        String sql = """
                SELECT unit_id, unit_status
                FROM blood_units
                WHERE unit_id = ?
                """;

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, unitId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    RecoveryBloodUnit unit = new RecoveryBloodUnit();
                    unit.setId(rs.getLong("unit_id"));
                    unit.setStatus(rs.getString("unit_status"));
                    return Optional.of(unit);
                }
            }

        } catch (Exception e) {
            System.out.println("Recovery unit search error: " + e.getMessage());
        }

        return Optional.empty();
    }

    public boolean updateStatus(Long unitId, String status) {
        String sql = """
                UPDATE blood_units
                SET unit_status = ?, updated_at = CURRENT_TIMESTAMP
                WHERE unit_id = ?
                """;

        try (Connection con = jdbcconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setLong(2, unitId);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            System.out.println("Recovery unit update error: " + e.getMessage());
            return false;
        }
    }
}
