
package backend;

import java.sql.Date;

/** One blood unit that is allowed for a request. */
public class MatchCandidate {

    private final int unitId;
    private final String unitCode;
    private final String donorGroup;
    private final Date expiryDate;
    private final int preferenceRank;

    public MatchCandidate(int unitId, String unitCode,
                          String donorGroup, Date expiryDate,
                          int preferenceRank) {

        this.unitId = unitId;
        this.unitCode = unitCode;
        this.donorGroup = donorGroup;
        this.expiryDate = expiryDate;
        this.preferenceRank = preferenceRank;
    }

    public int getUnitId() {
        return unitId;
    }

    public String getUnitCode() {
        return unitCode;
    }

    public String getDonorGroup() {
        return donorGroup;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public int getPreferenceRank() {
        return preferenceRank;
    }

    @Override
    public String toString() {
        return unitCode + "  group=" + donorGroup
                + "  expires=" + expiryDate
                + "  rank=" + preferenceRank;
    }
}
