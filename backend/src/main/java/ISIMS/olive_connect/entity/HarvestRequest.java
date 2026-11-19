package ISIMS.olive_connect.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

@Document(collection = "harvest_requests")
@CompoundIndexes({
        @CompoundIndex(name = "orchard_created_idx", def = "{'orchardId': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "farmer_created_idx", def = "{'farmerId': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "status_type_created_idx", def = "{'status': 1, 'requestType': 1, 'createdAt': -1}")
})
public class HarvestRequest {

    @Id
    private String id;

    private String orchardId;
    private String farmerId;
    private HarvestRequestType requestType;
    private LocalDate preferredDate;
    private Double estimatedQuantityKg;
    private String notes;
    private HarvestRequestStatus status = HarvestRequestStatus.PENDING;
    private String reviewComment;
    private String reviewedBy;
    private Instant reviewedAt;
    private Instant createdAt;
    private Instant updatedAt;
    

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOrchardId() { return orchardId; }
    public void setOrchardId(String orchardId) { this.orchardId = orchardId; }

    public String getFarmerId() { return farmerId; }
    public void setFarmerId(String farmerId) { this.farmerId = farmerId; }

    public HarvestRequestType getRequestType() { return requestType; }
    public void setRequestType(HarvestRequestType requestType) { this.requestType = requestType; }

    public LocalDate getPreferredDate() { return preferredDate; }
    public void setPreferredDate(LocalDate preferredDate) { this.preferredDate = preferredDate; }

    public Double getEstimatedQuantityKg() { return estimatedQuantityKg; }
    public void setEstimatedQuantityKg(Double estimatedQuantityKg) { this.estimatedQuantityKg = estimatedQuantityKg; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public HarvestRequestStatus getStatus() { return status; }
    public void setStatus(HarvestRequestStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public String getReviewComment() { return reviewComment; }
    public void setReviewComment(String reviewComment) { this.reviewComment = reviewComment; }

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }
}