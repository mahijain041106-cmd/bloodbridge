
package backend;

import java.sql.Timestamp;
import java.time.LocalDate;

public class BloodUnit {

    private long unitId;
    private String unitCode;
    private String bloodGroupCode;
    private int componentId;
    private LocalDate collectionDate;
    private LocalDate expiryDate;
    private UnitStatus status;
    private Long createdByUserId;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public BloodUnit() {
    }

    public BloodUnit(
            long unitId,
            String unitCode,
            String bloodGroupCode,
            int componentId,
            LocalDate collectionDate,
            LocalDate expiryDate,
            UnitStatus status,
            Long createdByUserId,
            Timestamp createdAt,
            Timestamp updatedAt) {

        this.unitId = unitId;
        this.unitCode = unitCode;
        this.bloodGroupCode = bloodGroupCode;
        this.componentId = componentId;
        this.collectionDate = collectionDate;
        this.expiryDate = expiryDate;
        this.status = status;
        this.createdByUserId = createdByUserId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long getUnitId() {
        return unitId;
    }

    public void setUnitId(long unitId) {
        this.unitId = unitId;
    }

    public String getUnitCode() {
        return unitCode;
    }

    public void setUnitCode(String unitCode) {
        this.unitCode = unitCode;
    }

    public String getBloodGroupCode() {
        return bloodGroupCode;
    }

    public void setBloodGroupCode(String bloodGroupCode) {
        this.bloodGroupCode = bloodGroupCode;
    }

    public int getComponentId() {
        return componentId;
    }

    public void setComponentId(int componentId) {
        this.componentId = componentId;
    }

    public LocalDate getCollectionDate() {
        return collectionDate;
    }

    public void setCollectionDate(LocalDate collectionDate) {
        this.collectionDate = collectionDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public UnitStatus getStatus() {
        return status;
    }

    public void setStatus(UnitStatus status) {
        this.status = status;
    }

    public Long getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(Long createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
