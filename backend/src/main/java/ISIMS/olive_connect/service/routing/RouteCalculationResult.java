package ISIMS.olive_connect.service.routing;

import java.util.ArrayList;
import java.util.List;

public class RouteCalculationResult {

    // Ordered [latitude, longitude] points along the road for Leaflet polylines
    private List<double[]> geometryPoints = new ArrayList<>();

    // One RouteLeg per consecutive waypoint transition (p0->p1, p1->p2, ...)
    private List<RouteLeg> legs = new ArrayList<>();

    private double totalDistanceKm;
    private long totalDurationMinutes;

    // true = straight lines, because the routing service gave no usable answer
    private boolean estimated;

    public List<double[]> getGeometryPoints() {
        return geometryPoints;
    }

    public void setGeometryPoints(List<double[]> geometryPoints) {
        this.geometryPoints = geometryPoints;
    }

    public List<RouteLeg> getLegs() {
        return legs;
    }

    public void setLegs(List<RouteLeg> legs) {
        this.legs = legs;
    }

    public boolean isEstimated() {
        return estimated;
    }

    public void setEstimated(boolean estimated) {
        this.estimated = estimated;
    }

    public double getTotalDistanceKm() {
        return totalDistanceKm;
    }

    public void setTotalDistanceKm(double totalDistanceKm) {
        this.totalDistanceKm = totalDistanceKm;
    }

    public long getTotalDurationMinutes() {
        return totalDurationMinutes;
    }

    public void setTotalDurationMinutes(long totalDurationMinutes) {
        this.totalDurationMinutes = totalDurationMinutes;
    }
}