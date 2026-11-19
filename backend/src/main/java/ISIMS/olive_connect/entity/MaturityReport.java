package ISIMS.olive_connect.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "maturity_reports")
@CompoundIndexes({
        @CompoundIndex(name = "orchard_reported_idx", def = "{'orchardId': 1, 'reportedAt': -1}"),
        @CompoundIndex(name = "farmer_reported_idx", def = "{'farmerId': 1, 'reportedAt': -1}")
})
public class MaturityReport {

    @Id
    private String id;

    private String orchardId;
    private String orchardName;
    private String farmerId;
    private MaturityLevel maturityLevel;
    private String notes;
    private boolean verified = false;
    private Instant reportedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOrchardId() { return orchardId; }
    public void setOrchardId(String orchardId) { this.orchardId = orchardId; }

    public String getOrchardName() { return orchardName; }
    public void setOrchardName(String orchardName) { this.orchardName = orchardName; }

    public String getFarmerId() { return farmerId; }
    public void setFarmerId(String farmerId) { this.farmerId = farmerId; }

    public MaturityLevel getMaturityLevel() { return maturityLevel; }
    public void setMaturityLevel(MaturityLevel maturityLevel) { this.maturityLevel = maturityLevel; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public Instant getReportedAt() { return reportedAt; }
    public void setReportedAt(Instant reportedAt) { this.reportedAt = reportedAt; }
}