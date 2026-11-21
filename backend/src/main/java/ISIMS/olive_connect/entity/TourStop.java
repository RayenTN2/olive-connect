package ISIMS.olive_connect.entity;

import java.time.Instant;

public class TourStop {

    private int visitOrder;
    private String harvestRequestId;
    private String orchardId;
    private double estimatedQuantityKg;

    // Populated/recalculated by routing & time estimation (Task 4)
    private Instant estimatedArrival;
    private Instant estimatedDeparture;
    private long harvestingDurationMinutes;
    private long travelDurationMinutesFromPrevious;
    private double travelDistanceKmFromPrevious;
    private String orchardName;
    private Double latitude;
    private Double longitude;

    public String getOrchardName() { return orchardName; }
    public void setOrchardName(String orchardName) { this.orchardName = orchardName; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public int getVisitOrder() { return visitOrder; }
    public void setVisitOrder(int visitOrder) { this.visitOrder = visitOrder; }

    public String getHarvestRequestId() { return harvestRequestId; }
    public void setHarvestRequestId(String harvestRequestId) { this.harvestRequestId = harvestRequestId; }

    public String getOrchardId() { return orchardId; }
    public void setOrchardId(String orchardId) { this.orchardId = orchardId; }

    public double getEstimatedQuantityKg() { return estimatedQuantityKg; }
    public void setEstimatedQuantityKg(double estimatedQuantityKg) { this.estimatedQuantityKg = estimatedQuantityKg; }

    public Instant getEstimatedArrival() { return estimatedArrival; }
    public void setEstimatedArrival(Instant estimatedArrival) { this.estimatedArrival = estimatedArrival; }

    public Instant getEstimatedDeparture() { return estimatedDeparture; }
    public void setEstimatedDeparture(Instant estimatedDeparture) { this.estimatedDeparture = estimatedDeparture; }

    public long getHarvestingDurationMinutes() { return harvestingDurationMinutes; }
    public void setHarvestingDurationMinutes(long harvestingDurationMinutes) { this.harvestingDurationMinutes = harvestingDurationMinutes; }

    public long getTravelDurationMinutesFromPrevious() { return travelDurationMinutesFromPrevious; }
    public void setTravelDurationMinutesFromPrevious(long travelDurationMinutesFromPrevious) { this.travelDurationMinutesFromPrevious = travelDurationMinutesFromPrevious; }

    public double getTravelDistanceKmFromPrevious() { return travelDistanceKmFromPrevious; }
    public void setTravelDistanceKmFromPrevious(double travelDistanceKmFromPrevious) { this.travelDistanceKmFromPrevious = travelDistanceKmFromPrevious; }
}