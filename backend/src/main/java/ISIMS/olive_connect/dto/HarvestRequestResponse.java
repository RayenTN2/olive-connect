package ISIMS.olive_connect.dto;

import ISIMS.olive_connect.entity.HarvestRequest;
import ISIMS.olive_connect.entity.HarvestRequestStatus;
import ISIMS.olive_connect.entity.HarvestRequestType;
import ISIMS.olive_connect.entity.MaturityLevel;
import ISIMS.olive_connect.entity.Orchard;
import ISIMS.olive_connect.entity.User;

import java.time.Instant;
import java.time.LocalDate;

public class HarvestRequestResponse {

    private String id;
    private String orchardId;
    private String orchardName;
    private MaturityLevel orchardMaturity;
    private Double latitude;
    private Double longitude;

    private String farmerId;
    private String farmerName;
    private String farmerPhone;
    private String farmerEmail;

    private HarvestRequestType requestType;
    private LocalDate preferredDate;
    private Double estimatedQuantityKg;
    private String notes;
    private HarvestRequestStatus status;
    private Instant submittedAt;
    private Instant updatedAt;
    private String reviewComment;
    private String reviewedBy;
    private Instant reviewedAt;
    private boolean tourEligible;

    public static HarvestRequestResponse fromEntity(HarvestRequest request, Orchard orchard) {
        return fromEntity(request, orchard, null);
    }

    public static HarvestRequestResponse fromEntity(HarvestRequest request, Orchard orchard, User farmer) {
        HarvestRequestResponse res = new HarvestRequestResponse();
        res.setId(request.getId());
        res.setOrchardId(request.getOrchardId());
        res.setFarmerId(request.getFarmerId());
        res.setRequestType(request.getRequestType());
        res.setPreferredDate(request.getPreferredDate());
        res.setEstimatedQuantityKg(request.getEstimatedQuantityKg());
        res.setNotes(request.getNotes());
        res.setStatus(request.getStatus());
        res.setSubmittedAt(request.getCreatedAt());
        res.setUpdatedAt(request.getUpdatedAt());
        res.setReviewComment(request.getReviewComment());
        res.setReviewedBy(request.getReviewedBy());
        res.setReviewedAt(request.getReviewedAt());
        res.setTourEligible(request.getStatus() == HarvestRequestStatus.APPROVED);

        if (orchard != null) {
            res.setOrchardName(orchard.getName());
            res.setOrchardMaturity(orchard.getCurrentMaturity());
            if (orchard.getLocation() != null) {
                res.setLatitude(orchard.getLocation().getY());
                res.setLongitude(orchard.getLocation().getX());
            }
        }

        if (farmer != null) {
            res.setFarmerName(farmer.getFullName());
            res.setFarmerPhone(farmer.getPhone());
            res.setFarmerEmail(farmer.getEmail());
        }

        return res;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOrchardId() { return orchardId; }
    public void setOrchardId(String orchardId) { this.orchardId = orchardId; }

    public String getOrchardName() { return orchardName; }
    public void setOrchardName(String orchardName) { this.orchardName = orchardName; }

    public MaturityLevel getOrchardMaturity() { return orchardMaturity; }
    public void setOrchardMaturity(MaturityLevel orchardMaturity) { this.orchardMaturity = orchardMaturity; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getFarmerId() { return farmerId; }
    public void setFarmerId(String farmerId) { this.farmerId = farmerId; }

    public String getFarmerName() { return farmerName; }
    public void setFarmerName(String farmerName) { this.farmerName = farmerName; }

    public String getFarmerPhone() { return farmerPhone; }
    public void setFarmerPhone(String farmerPhone) { this.farmerPhone = farmerPhone; }

    public String getFarmerEmail() { return farmerEmail; }
    public void setFarmerEmail(String farmerEmail) { this.farmerEmail = farmerEmail; }

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

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public String getReviewComment() { return reviewComment; }
    public void setReviewComment(String reviewComment) { this.reviewComment = reviewComment; }

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }

    public boolean isTourEligible() { return tourEligible; }
    public void setTourEligible(boolean tourEligible) { this.tourEligible = tourEligible; }
}