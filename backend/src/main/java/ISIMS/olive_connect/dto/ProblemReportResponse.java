package ISIMS.olive_connect.dto;

import ISIMS.olive_connect.entity.Orchard;
import ISIMS.olive_connect.entity.ProblemReport;
import ISIMS.olive_connect.entity.ProblemStatus;
import ISIMS.olive_connect.entity.ProblemType;

import java.time.Instant;

public class ProblemReportResponse {

    private String id;
    private String orchardId;
    private String orchardName;
    private String farmerId;
    private ProblemType problemType;
    private String description;
    private Double latitude;
    private Double longitude;
    private ProblemStatus status;
    private Instant reportedAt;

    public static ProblemReportResponse fromEntity(ProblemReport report, Orchard orchard) {
        ProblemReportResponse res = new ProblemReportResponse();
        res.setId(report.getId());
        res.setOrchardId(report.getOrchardId());
        res.setFarmerId(report.getFarmerId());
        res.setProblemType(report.getProblemType());
        res.setDescription(report.getDescription());
        if (orchard != null) {
            res.setOrchardName(orchard.getName());
            if (orchard.getLocation() != null) {
                res.setLatitude(orchard.getLocation().getY());
                res.setLongitude(orchard.getLocation().getX());
            }
        }
        res.setStatus(report.getStatus());
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

    public ProblemType getProblemType() { return problemType; }
    public void setProblemType(ProblemType problemType) { this.problemType = problemType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public ProblemStatus getStatus() { return status; }
    public void setStatus(ProblemStatus status) { this.status = status; }

    public Instant getReportedAt() { return reportedAt; }
    public void setReportedAt(Instant reportedAt) { this.reportedAt = reportedAt; }
}