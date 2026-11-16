
package backend;

import java.sql.Timestamp;

/** One audit_logs row, ready to display. */
public class AuditEntry {

    private final long auditId;
    private final Timestamp createdAt;
    private final String actor;
    private final String actionType;
    private final String entityType;
    private final Integer entityId;
    private final String oldValues;
    private final String newValues;
    private final String details;

    public AuditEntry(long auditId, Timestamp createdAt, String actor,
                      String actionType, String entityType, Integer entityId,
                      String oldValues, String newValues, String details) {

        this.auditId = auditId;
        this.createdAt = createdAt;
        this.actor = actor;
        this.actionType = actionType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.oldValues = oldValues;
        this.newValues = newValues;
        this.details = details;
    }

    public long getAuditId() {
        return auditId;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public String getActor() {
        return actor;
    }

    public String getActionType() {
        return actionType;
    }

    public String getEntityType() {
        return entityType;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public String getOldValues() {
        return oldValues;
    }

    public String getNewValues() {
        return newValues;
    }

    public String getDetails() {
        return details;
    }

    @Override
    public String toString() {
        return "#" + auditId + "  " + createdAt + "  " + actor
                + "  " + actionType + "  " + entityType
                + (entityId == null ? "" : "#" + entityId)
                + (details == null ? "" : "  - " + details);
    }
}
