package ISIMS.olive_connect.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "problem_reports")
@CompoundIndexes({
        @CompoundIndex(name = "orchard_reported_idx", def = "{'orchardId': 1, 'reportedAt': -1}"),
        @CompoundIndex(name = "farmer_reported_idx", def = "{'farmerId': 1, 'reportedAt': -1}"),
        @CompoundIndex(name = "status_reported_idx", def = "{'status': 1, 'reportedAt': -1}")
})
public class ProblemReport {

    @Id
    private String id;

    private String orchardId;
    private String farmerId;
    private ProblemType problemType;
    private String description;
    private ProblemStatus status = ProblemStatus.PENDING_REVIEW;
    private Instant reportedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOrchardId() { return orchardId; }
    public void setOrchardId(String orchardId) { this.orchardId = orchardId; }

    public String getFarmerId() { return farmerId; }
    public void setFarmerId(String farmerId) { this.farmerId = farmerId; }

    public ProblemType getProblemType() { return problemType; }
    public void setProblemType(ProblemType problemType) { this.problemType = problemType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ProblemStatus getStatus() { return status; }
    public void setStatus(ProblemStatus status) { this.status = status; }

    public Instant getReportedAt() { return reportedAt; }
    public void setReportedAt(Instant reportedAt) { this.reportedAt = reportedAt; }
}