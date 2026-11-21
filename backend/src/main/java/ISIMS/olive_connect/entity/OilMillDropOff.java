package ISIMS.olive_connect.entity;

import java.time.Instant;

public class OilMillDropOff {

    private String oilMillId;
    private double allocatedQuantityKg;
    private Instant estimatedArrival;
    private long travelDurationMinutesFromPrevious;
    private double travelDistanceKmFromPrevious;
    private String oilMillName;
    private Double latitude;
    private Double longitude;

    public String getOilMillName() { return oilMillName; }
    public void setOilMillName(String oilMillName) { this.oilMillName = oilMillName; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getOilMillId() { return oilMillId; }
    public void setOilMillId(String oilMillId) { this.oilMillId = oilMillId; }

    public double getAllocatedQuantityKg() { return allocatedQuantityKg; }
    public void setAllocatedQuantityKg(double allocatedQuantityKg) { this.allocatedQuantityKg = allocatedQuantityKg; }

    public Instant getEstimatedArrival() { return estimatedArrival; }
    public void setEstimatedArrival(Instant estimatedArrival) { this.estimatedArrival = estimatedArrival; }

    public long getTravelDurationMinutesFromPrevious() { return travelDurationMinutesFromPrevious; }
    public void setTravelDurationMinutesFromPrevious(long travelDurationMinutesFromPrevious) { this.travelDurationMinutesFromPrevious = travelDurationMinutesFromPrevious; }

    public double getTravelDistanceKmFromPrevious() { return travelDistanceKmFromPrevious; }
    public void setTravelDistanceKmFromPrevious(double travelDistanceKmFromPrevious) { this.travelDistanceKmFromPrevious = travelDistanceKmFromPrevious; }
}