
package backend;

import java.sql.Timestamp;

public class RuleVersion {

    private long ruleVersionId;
    private String versionCode;
    private int versionNumber;
    private String sourceDescription;
    private String approvalNote;
    private String ruleStatus;
    private Long createdByUserId;
    private Long approvedByUserId;
    private Timestamp effectiveFrom;
    private Timestamp effectiveTo;
    private Timestamp createdAt;

    public RuleVersion() {
    }

    public RuleVersion(
            long ruleVersionId,
            String versionCode,
            int versionNumber,
            String sourceDescription,
            String approvalNote,
            String ruleStatus,
            Long createdByUserId,
            Long approvedByUserId,
            Timestamp effectiveFrom,
            Timestamp effectiveTo,
            Timestamp createdAt) {

        this.ruleVersionId = ruleVersionId;
        this.versionCode = versionCode;
        this.versionNumber = versionNumber;
        this.sourceDescription = sourceDescription;
        this.approvalNote = approvalNote;
        this.ruleStatus = ruleStatus;
        this.createdByUserId = createdByUserId;
        this.approvedByUserId = approvedByUserId;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.createdAt = createdAt;
    }

    public long getRuleVersionId() {
        return ruleVersionId;
    }

    public void setRuleVersionId(long ruleVersionId) {
        this.ruleVersionId = ruleVersionId;
    }

    public String getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(String versionCode) {
        this.versionCode = versionCode;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(int versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getSourceDescription() {
        return sourceDescription;
    }

    public void setSourceDescription(String sourceDescription) {
        this.sourceDescription = sourceDescription;
    }

    public String getApprovalNote() {
        return approvalNote;
    }

    public void setApprovalNote(String approvalNote) {
        this.approvalNote = approvalNote;
    }

    public String getRuleStatus() {
        return ruleStatus;
    }

    public void setRuleStatus(String ruleStatus) {
        this.ruleStatus = ruleStatus;
    }

    public Long getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(Long createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public Long getApprovedByUserId() {
        return approvedByUserId;
    }

    public void setApprovedByUserId(Long approvedByUserId) {
        this.approvedByUserId = approvedByUserId;
    }

    public Timestamp getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(Timestamp effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public Timestamp getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(Timestamp effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(ruleStatus);
    }
}
