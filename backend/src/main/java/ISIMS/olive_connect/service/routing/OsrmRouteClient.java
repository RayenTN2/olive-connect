package ISIMS.olive_connect.service.routing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode; // Jackson 3: the one Spring Boot 4 reads HTTP answers with

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

@Component
public class OsrmRouteClient {

    private static final Logger log = LoggerFactory.getLogger(OsrmRouteClient.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);
    private static final double ROAD_FACTOR = 1.25;
    private static final double ASSUMED_SPEED_KMH = 45.0;

    private final RestClient restClient;
    private final String baseUrl;

    @Autowired
    public OsrmRouteClient(@Value("${olive.routing.osrm-base-url:https://router.project-osrm.org}") String baseUrl) {
        this(baseUrl, CONNECT_TIMEOUT, READ_TIMEOUT);
    }

    // Package-visible so tests don't wait for the real timeouts
    OsrmRouteClient(String baseUrl, Duration connectTimeout, Duration readTimeout) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(connectTimeout).build());
        requestFactory.setReadTimeout(readTimeout);
        requestFactory.enableCompression(false); // OSRM's "deflate" answers fail with "incorrect header check"
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    public RouteCalculationResult calculateRoute(List<GeoPoint> waypoints) {
        RouteCalculationResult result = new RouteCalculationResult();
        if (waypoints == null || waypoints.size() < 2) {
            if (waypoints != null && waypoints.size() == 1) {
                GeoPoint single = waypoints.get(0);
                result.getGeometryPoints().add(new double[]{single.getLatitude(), single.getLongitude()});
            }
            return result;
        }

        try {
            return queryOsrm(waypoints);
        } catch (Exception ex) {
            log.warn("OSRM route unavailable, using a straight-line estimate: {}", ex.toString());
            return fallbackHaversineRoute(waypoints);
        }
    }

    private RouteCalculationResult queryOsrm(List<GeoPoint> waypoints) {
        StringBuilder coords = new StringBuilder();
        for (int i = 0; i < waypoints.size(); i++) {
            if (i > 0) {
                coords.append(';');
            }
            GeoPoint p = waypoints.get(i);
            coords.append(String.format(Locale.US, "%.6f,%.6f", p.getLongitude(), p.getLatitude()));
        }

        String url = baseUrl + "/route/v1/driving/" + coords + "?overview=full&geometries=geojson&steps=false";
        JsonNode root = restClient.get().uri(url).retrieve().body(JsonNode.class);

        if (root == null || !"Ok".equalsIgnoreCase(root.path("code").asString())) {
            throw new IllegalStateException("OSRM returned non-Ok response");
        }

        JsonNode routeNode = root.path("routes").get(0);
        if (routeNode == null) {
            throw new IllegalStateException("OSRM returned no routes");
        }

        RouteCalculationResult result = new RouteCalculationResult();

        JsonNode coordsNode = routeNode.path("geometry").path("coordinates");
        if (coordsNode.isArray()) {
            for (JsonNode pt : coordsNode) {
                double lon = pt.get(0).asDouble();
                double lat = pt.get(1).asDouble();
                result.getGeometryPoints().add(new double[]{lat, lon});
            }
        }

        if (result.getGeometryPoints().size() < 2) {
            throw new IllegalStateException("OSRM returned no route geometry");
        }

        JsonNode legsNode = routeNode.path("legs");
        double totalKm = 0.0;
        long totalMinutes = 0L;

        if (legsNode.isArray()) {
            for (JsonNode legNode : legsNode) {
                double distanceKm = round2(legNode.path("distance").asDouble() / 1000.0);
                long durationMinutes = Math.max(1L, Math.round(legNode.path("duration").asDouble() / 60.0));
                result.getLegs().add(new RouteLeg(distanceKm, durationMinutes));
                totalKm += distanceKm;
                totalMinutes += durationMinutes;
            }
        }

        if (result.getLegs().size() != waypoints.size() - 1) {
            throw new IllegalStateException("OSRM returned " + result.getLegs().size() + " legs for " + waypoints.size() + " points");
        }

        result.setTotalDistanceKm(round2(totalKm));
        result.setTotalDurationMinutes(totalMinutes);
        return result;
    }

    private RouteCalculationResult fallbackHaversineRoute(List<GeoPoint> waypoints) {
        RouteCalculationResult result = new RouteCalculationResult();
        result.setEstimated(true);
        double totalKm = 0.0;
        long totalMinutes = 0L;

        for (int i = 0; i < waypoints.size(); i++) {
            GeoPoint p = waypoints.get(i);
            result.getGeometryPoints().add(new double[]{p.getLatitude(), p.getLongitude()});
            if (i > 0) {
                GeoPoint prev = waypoints.get(i - 1);
                double legKm = round2(haversineKm(prev.getLatitude(), prev.getLongitude(), p.getLatitude(), p.getLongitude()) * ROAD_FACTOR);
                long legMinutes = Math.max(1L, Math.round((legKm / ASSUMED_SPEED_KMH) * 60.0));
                result.getLegs().add(new RouteLeg(legKm, legMinutes));
                totalKm += legKm;
                totalMinutes += legMinutes;
            }
        }

        result.setTotalDistanceKm(round2(totalKm));
        result.setTotalDurationMinutes(totalMinutes);
        return result;
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}