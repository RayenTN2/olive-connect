package ISIMS.olive_connect.dto;

import ISIMS.olive_connect.entity.MaturityLevel;
import ISIMS.olive_connect.entity.MaturityReport;

import java.time.Instant;

public class MaturityReportResponse {

    private String id;
    private String orchardId;
    private String orchardName;
    private String farmerId;
    private MaturityLevel maturityLevel;
    private String notes;
    private boolean verified;
    private Instant reportedAt;

    public static MaturityReportResponse fromEntity(MaturityReport report) {
        MaturityReportResponse res = new MaturityReportResponse();
        res.setId(report.getId());
        res.setOrchardId(report.getOrchardId());
        res.setOrchardName(report.getOrchardName());
        res.setFarmerId(report.getFarmerId());
        res.setMaturityLevel(report.getMaturityLevel());
        res.setNotes(report.getNotes());
        res.setVerified(report.isVerified());
        res.setReportedAt(report.getReportedAt());
        return res;
    }

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