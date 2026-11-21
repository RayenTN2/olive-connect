package ISIMS.olive_connect.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "collection_tours")
public class CollectionTour {

    @Id
    private String id;

    private String title;
    private String notes;
    private String teamLeaderId;

    private CollectionTourStatus status = CollectionTourStatus.PLANNED;
    private Instant plannedStartTime;
    private Instant estimatedEndTime;

    private List<TourStop> stops = new ArrayList<>();
    private List<OilMillDropOff> dropOffs = new ArrayList<>();

    private List<String> assignedWorkerIds = new ArrayList<>();
    private List<String> assignedEquipmentIds = new ArrayList<>();

    private double totalQuantityKg;
    private double totalAllocatedQuantityKg;
    private double totalDistanceKm;
    private long totalTravelDurationMinutes;
    private long totalHarvestingDurationMinutes;
    private long totalTourDurationMinutes;

    private List<List<Double>> routeGeometry = new ArrayList<>();

    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant confirmedAt;
    private Instant cancelledAt;
    private String cancellationReason;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getTeamLeaderId() { return teamLeaderId; }
    public void setTeamLeaderId(String teamLeaderId) { this.teamLeaderId = teamLeaderId; }

    public CollectionTourStatus getStatus() { return status; }
    public void setStatus(CollectionTourStatus status) { this.status = status; }

    public Instant getPlannedStartTime() { return plannedStartTime; }
    public void setPlannedStartTime(Instant plannedStartTime) { this.plannedStartTime = plannedStartTime; }

    public Instant getEstimatedEndTime() { return estimatedEndTime; }
    public void setEstimatedEndTime(Instant estimatedEndTime) { this.estimatedEndTime = estimatedEndTime; }

    public List<TourStop> getStops() { return stops; }
    public void setStops(List<TourStop> stops) { this.stops = stops; }

    public List<OilMillDropOff> getDropOffs() { return dropOffs; }
    public void setDropOffs(List<OilMillDropOff> dropOffs) { this.dropOffs = dropOffs; }

    public List<String> getAssignedWorkerIds() { return assignedWorkerIds; }
    public void setAssignedWorkerIds(List<String> assignedWorkerIds) { this.assignedWorkerIds = assignedWorkerIds; }

    public List<String> getAssignedEquipmentIds() { return assignedEquipmentIds; }
    public void setAssignedEquipmentIds(List<String> assignedEquipmentIds) { this.assignedEquipmentIds = assignedEquipmentIds; }

    public double getTotalQuantityKg() { return totalQuantityKg; }
    public void setTotalQuantityKg(double totalQuantityKg) { this.totalQuantityKg = totalQuantityKg; }

    public double getTotalAllocatedQuantityKg() { return totalAllocatedQuantityKg; }
    public void setTotalAllocatedQuantityKg(double totalAllocatedQuantityKg) { this.totalAllocatedQuantityKg = totalAllocatedQuantityKg; }

    public double getTotalDistanceKm() { return totalDistanceKm; }
    public void setTotalDistanceKm(double totalDistanceKm) { this.totalDistanceKm = totalDistanceKm; }

    public long getTotalTravelDurationMinutes() { return totalTravelDurationMinutes; }
    public void setTotalTravelDurationMinutes(long totalTravelDurationMinutes) { this.totalTravelDurationMinutes = totalTravelDurationMinutes; }

    public long getTotalHarvestingDurationMinutes() { return totalHarvestingDurationMinutes; }
    public void setTotalHarvestingDurationMinutes(long totalHarvestingDurationMinutes) { this.totalHarvestingDurationMinutes = totalHarvestingDurationMinutes; }

    public long getTotalTourDurationMinutes() { return totalTourDurationMinutes; }
    public void setTotalTourDurationMinutes(long totalTourDurationMinutes) { this.totalTourDurationMinutes = totalTourDurationMinutes; }

    public List<List<Double>> getRouteGeometry() { return routeGeometry; }
    public void setRouteGeometry(List<List<Double>> routeGeometry) { this.routeGeometry = routeGeometry; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Instant getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(Instant confirmedAt) { this.confirmedAt = confirmedAt; }

    public Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Instant cancelledAt) { this.cancelledAt = cancelledAt; }

    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }
}