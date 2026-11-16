
package backend;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InventoryController {

    private final InventoryService service;

    public InventoryController() {
        this.service = new InventoryService();
    }

    public Map<String, Object> add(
            String unitCode, String bloodGroupCode,
            int componentId, LocalDate collectionDate,
            LocalDate expiryDate, long createdByUserId) {

        Map<String, Object> response = new LinkedHashMap<>();

        if (!isValidInput(unitCode, bloodGroupCode,
                collectionDate, expiryDate)
                || componentId <= 0 || createdByUserId <= 0) {
            response.put("success", false);
            response.put("message", "Invalid inventory details.");
            return response;
        }

        if (!expiryDate.isAfter(collectionDate)) {
            response.put("success", false);
            response.put("message",
                    "Expiry date must be after collection date.");
            return response;
        }

        boolean added = service.add(unitCode, bloodGroupCode,
                componentId, collectionDate, expiryDate, createdByUserId);

        response.put("success", added);
        response.put("message", added
                ? "Blood unit added successfully."
                : "Could not add blood unit.");

        return response;
    }

    public List<Map<String, Object>> getAll() {
        return service.getAll();
    }

    public Map<String, Object> getById(long id) {
        return id > 0 ? service.getById(id) : null;
    }

    public List<Map<String, Object>> getByStatus(String status) {
        return service.getByStatus(status);
    }

    public List<Map<String, Object>> getByCategory(String bloodGroupCode) {
        return service.getByBloodGroup(bloodGroupCode);
    }

    public Map<String, Object> update(
            long id, String unitCode, String bloodGroupCode,
            int componentId, LocalDate collectionDate,
            LocalDate expiryDate) {

        Map<String, Object> response = new LinkedHashMap<>();

        if (id <= 0 || componentId <= 0
                || !isValidInput(unitCode, bloodGroupCode,
                        collectionDate, expiryDate)) {
            response.put("success", false);
            response.put("message", "Invalid inventory details.");
            return response;
        }

        if (!expiryDate.isAfter(collectionDate)) {
            response.put("success", false);
            response.put("message",
                    "Expiry date must be after collection date.");
            return response;
        }

        boolean updated = service.update(id, unitCode,
                bloodGroupCode, componentId,
                collectionDate, expiryDate);

        response.put("success", updated);
        response.put("message", updated
                ? "Blood unit updated successfully."
                : "Update failed. Unit may not exist or may be reserved.");

        return response;
    }

    public Map<String, String> delete(long id) {
        Map<String, String> response = new LinkedHashMap<>();

        boolean deleted = id > 0 && service.delete(id);

        response.put("message", deleted
                ? "Inventory unit deleted."
                : "Delete failed. Unit may not exist or may be reserved.");

        return response;
    }

    public Map<String, Long> count() {
        Map<String, Long> counts = new LinkedHashMap<>();

        counts.put("total", service.countAll());
        counts.put("available", service.countByStatus("AVAILABLE"));
        counts.put("reserved", service.countByStatus("RESERVED"));
        counts.put("expired", service.countByStatus("EXPIRED"));
        counts.put("inactive", service.countByStatus("INACTIVE"));

        return counts;
    }

    public Map<String, Integer> markExpired() {
        Map<String, Integer> response = new LinkedHashMap<>();
        response.put("updatedUnits", service.markExpiredUnits());
        return response;
    }

    private boolean isValidInput(
            String unitCode, String bloodGroupCode,
            LocalDate collectionDate, LocalDate expiryDate) {

        return unitCode != null && !unitCode.isBlank()
                && bloodGroupCode != null && !bloodGroupCode.isBlank()
                && collectionDate != null && expiryDate != null;
    }
}
