package ISIMS.olive_connect.dto;

import ISIMS.olive_connect.entity.CollectionTour;
import ISIMS.olive_connect.entity.CollectionTourStatus;
import ISIMS.olive_connect.entity.OilMillDropOff;
import ISIMS.olive_connect.entity.TourStop;

import java.time.Instant;
import java.util.List;

public class CollectionTourResponse {

    private String id;
    private String title;
    private String notes;
    private String teamLeaderId;
    private String teamLeaderName;
    private CollectionTourStatus status;
    private Instant plannedStartTime;
    private Instant estimatedEndTime;
    private List<TourStop> stops;
    private List<OilMillDropOff> dropOffs;
    private List<String> assignedWorkerIds;
    private List<String> assignedEquipmentIds;
    // Details of the assigned resources (filled by CollectionTourService)
    private List<WorkerResponse> assignedWorkers;
    private List<EquipmentResponse> assignedEquipment;
    private double totalQuantityKg;
    private double totalAllocatedQuantityKg;
    private double totalDistanceKm;
    private long totalTravelDurationMinutes;
    private long totalHarvestingDurationMinutes;
    private long totalTourDurationMinutes;
    private List<List<Double>> routeGeometry;
    private Boolean routeEstimated;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant confirmedAt;
    private Instant cancelledAt;
    private String cancellationReason;

    public static CollectionTourResponse fromEntity(CollectionTour tour) {
        CollectionTourResponse res = new CollectionTourResponse();
        res.setId(tour.getId());
        res.setTitle(tour.getTitle());
        res.setNotes(tour.getNotes());
        res.setTeamLeaderId(tour.getTeamLeaderId());
        res.setStatus(tour.getStatus());
        res.setPlannedStartTime(tour.getPlannedStartTime());
        res.setEstimatedEndTime(tour.getEstimatedEndTime());
        res.setStops(tour.getStops());
        res.setDropOffs(tour.getDropOffs());
        res.setAssignedWorkerIds(tour.getAssignedWorkerIds());
        res.setAssignedEquipmentIds(tour.getAssignedEquipmentIds());
        res.setTotalQuantityKg(tour.getTotalQuantityKg());
        res.setTotalAllocatedQuantityKg(tour.getTotalAllocatedQuantityKg());
        res.setTotalDistanceKm(tour.getTotalDistanceKm());
        res.setTotalTravelDurationMinutes(tour.getTotalTravelDurationMinutes());
        res.setTotalHarvestingDurationMinutes(tour.getTotalHarvestingDurationMinutes());
        res.setTotalTourDurationMinutes(tour.getTotalTourDurationMinutes());
        res.setRouteGeometry(tour.getRouteGeometry());
        res.setRouteEstimated(tour.getRouteEstimated());
        res.setCreatedBy(tour.getCreatedBy());
        res.setCreatedAt(tour.getCreatedAt());
        res.setUpdatedAt(tour.getUpdatedAt());
        res.setConfirmedAt(tour.getConfirmedAt());
        res.setCancelledAt(tour.getCancelledAt());
        res.setCancellationReason(tour.getCancellationReason());
        return res;
    }

    public List<WorkerResponse> getAssignedWorkers() { return assignedWorkers; }
    public void setAssignedWorkers(List<WorkerResponse> assignedWorkers) { this.assignedWorkers = assignedWorkers; }

    public List<EquipmentResponse> getAssignedEquipment() { return assignedEquipment; }
    public void setAssignedEquipment(List<EquipmentResponse> assignedEquipment) { this.assignedEquipment = assignedEquipment; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getTeamLeaderId() { return teamLeaderId; }
    public void setTeamLeaderId(String teamLeaderId) { this.teamLeaderId = teamLeaderId; }

    public String getTeamLeaderName() { return teamLeaderName; }
    public void setTeamLeaderName(String teamLeaderName) { this.teamLeaderName = teamLeaderName; }

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

    public Boolean getRouteEstimated() { return routeEstimated; }
    public void setRouteEstimated(Boolean routeEstimated) { this.routeEstimated = routeEstimated; }

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