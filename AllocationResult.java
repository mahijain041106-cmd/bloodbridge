
package backend;

import java.util.List;

/** What the matching engine found for one request.
 * Nothing is reserved yet.
 */
public class AllocationResult {

    private final String recipientGroup;
    private final String component;
    private final int quantity;
    private final int ruleVersionId;
    private final String ruleVersionName;
    private final List<MatchCandidate> candidates;

    public AllocationResult(
            String recipientGroup,
            String component,
            int quantity,
            int ruleVersionId,
            String ruleVersionName,
            List<MatchCandidate> candidates) {

        this.recipientGroup = recipientGroup;
        this.component = component;
        this.quantity = quantity;
        this.ruleVersionId = ruleVersionId;
        this.ruleVersionName = ruleVersionName;
        this.candidates = candidates;
    }

    public String getRecipientGroup() {
        return recipientGroup;
    }

    public String getComponent() {
        return component;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getRuleVersionId() {
        return ruleVersionId;
    }

    public String getRuleVersionName() {
        return ruleVersionName;
    }

    public List<MatchCandidate> getCandidates() {
        return candidates;
    }

    public boolean isEnough() {
        return candidates.size() >= quantity;
    }

    public List<MatchCandidate> getSelectedUnits() {
        return candidates.subList(
                0, Math.min(quantity, candidates.size()));
    }

    public String getOutcome() {
        if (isEnough()) {
            return "FULL_MATCH";
        }

        return candidates.isEmpty()
                ? "NO_MATCH"
                : "PARTIAL_MATCH";
    }
}
