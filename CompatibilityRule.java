
package backend;

import java.sql.Timestamp;

public class CompatibilityRule {

    private long compatibilityRuleId;
    private long ruleVersionId;
    private String requestedGroupCode;
    private String unitGroupCode;
    private int componentId;
    private boolean allowed;
    private String ruleNote;
    private Timestamp createdAt;

    public CompatibilityRule() {
    }

    public CompatibilityRule(
            long compatibilityRuleId,
            long ruleVersionId,
            String requestedGroupCode,
            String unitGroupCode,
            int componentId,
            boolean allowed,
            String ruleNote,
            Timestamp createdAt) {

        this.compatibilityRuleId = compatibilityRuleId;
        this.ruleVersionId = ruleVersionId;
        this.requestedGroupCode = requestedGroupCode;
        this.unitGroupCode = unitGroupCode;
        this.componentId = componentId;
        this.allowed = allowed;
        this.ruleNote = ruleNote;
        this.createdAt = createdAt;
    }

    public long getCompatibilityRuleId() {
        return compatibilityRuleId;
    }

    public void setCompatibilityRuleId(long compatibilityRuleId) {
        this.compatibilityRuleId = compatibilityRuleId;
    }

    public long getRuleVersionId() {
        return ruleVersionId;
    }

    public void setRuleVersionId(long ruleVersionId) {
        this.ruleVersionId = ruleVersionId;
    }

    public String getRequestedGroupCode() {
        return requestedGroupCode;
    }

    public void setRequestedGroupCode(String requestedGroupCode) {
        this.requestedGroupCode = requestedGroupCode;
    }

    public String getUnitGroupCode() {
        return unitGroupCode;
    }

    public void setUnitGroupCode(String unitGroupCode) {
        this.unitGroupCode = unitGroupCode;
    }

    public int getComponentId() {
        return componentId;
    }

    public void setComponentId(int componentId) {
        this.componentId = componentId;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
    }

    public String getRuleNote() {
        return ruleNote;
    }

    public void setRuleNote(String ruleNote) {
        this.ruleNote = ruleNote;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
