package ISIMS.olive_connect.service.routing;

public class RouteLeg {

    private final double distanceKm;
    private final long durationMinutes;

    public RouteLeg(double distanceKm, long durationMinutes) {
        this.distanceKm = distanceKm;
        this.durationMinutes = durationMinutes;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public long getDurationMinutes() {
        return durationMinutes;
    }
}