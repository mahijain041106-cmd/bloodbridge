
package backend;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CompatibilityController {

    private final CompatibilityService service;

    public CompatibilityController() {
        this.service = new CompatibilityService();
    }

    // Add a compatibility rule
    public Map<String, Object> add(
            long ruleVersionId,
            String requestedGroupCode,
            String unitGroupCode,
            int componentId,
            boolean allowed,
            String ruleNote) {

        Map<String, Object> response = new LinkedHashMap<>();

        if (ruleVersionId <= 0 || componentId <= 0
                || requestedGroupCode == null || requestedGroupCode.isBlank()
                || unitGroupCode == null || unitGroupCode.isBlank()) {

            response.put("success", false);
            response.put("message", "Invalid compatibility rule details.");
            return response;
        }

        boolean added = service.add(
                ruleVersionId,
                requestedGroupCode,
                unitGroupCode,
                componentId,
                allowed,
                ruleNote
        );

        response.put("success", added);
        response.put("message", added
                ? "Compatibility rule added successfully."
                : "Could not add compatibility rule.");

        return response;
    }

    // Get all compatibility rules
    public List<CompatibilityRule> getAll() {
        return service.getAll();
    }

    // Get a rule by ID
    public CompatibilityRule getById(long id) {
        if (id <= 0) {
            return null;
        }

        return service.getById(id);
    }

    // Get rules by rule version
    public List<CompatibilityRule> getByVersion(long ruleVersionId) {
        if (ruleVersionId <= 0) {
            return List.of();
        }

        return service.getByVersion(ruleVersionId);
    }

    // Check whether two blood groups are compatible
    public Map<String, Object> checkCompatibility(
            long ruleVersionId,
            String requestedGroupCode,
            String unitGroupCode,
            int componentId) {

        Map<String, Object> response = new LinkedHashMap<>();

        if (ruleVersionId <= 0 || componentId <= 0
                || requestedGroupCode == null || requestedGroupCode.isBlank()
                || unitGroupCode == null || unitGroupCode.isBlank()) {

            response.put("success", false);
            response.put("compatible", false);
            response.put("message", "Invalid compatibility check details.");
            return response;
        }

        boolean compatible = service.isCompatible(
                ruleVersionId,
                requestedGroupCode,
                unitGroupCode,
                componentId
        );

        response.put("success", true);
        response.put("compatible", compatible);
        response.put("message", compatible
                ? "The configured rule allows this match."
                : "No allowed compatibility rule was found.");

        return response;
    }
}
