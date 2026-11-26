package ISIMS.olive_connect.service.routing;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

// A local server plays OSRM: no test needs the internet
class OsrmRouteClientTest {

    private static final List<GeoPoint> POINTS = List.of(
            new GeoPoint(34.742, 10.7615), new GeoPoint(34.83, 10.62), new GeoPoint(34.75, 10.70));
    private static final String ROUTE = """
            {"code":"Ok","waypoints":[],"routes":[{
              "geometry":{"type":"LineString","coordinates":[[10.7615,34.742],[10.70,34.78],[10.62,34.83],[10.66,34.79],[10.70,34.75]]},
              "legs":[{"distance":20365.1,"duration":1900},{"distance":15361.6,"duration":1490.9}]}]}
            """;

    private final CountDownLatch release = new CountDownLatch(1);
    private HttpServer server;
    private volatile String path;
    private volatile String acceptEncoding;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.setExecutor(Executors.newCachedThreadPool(task -> {
            Thread thread = new Thread(task);
            thread.setDaemon(true);
            return thread;
        }));
        server.start();
    }

    @AfterEach
    void tearDown() {
        release.countDown();
        server.stop(0);
    }

    private OsrmRouteClient client() {
        return new OsrmRouteClient("http://localhost:" + server.getAddress().getPort() + "/",
                Duration.ofSeconds(2), Duration.ofMillis(500));
    }

    private RouteCalculationResult answer(int status, String contentType, String body) {
        server.createContext("/", exchange -> {
            path = exchange.getRequestURI().toString();
            acceptEncoding = exchange.getRequestHeaders().getFirst("Accept-Encoding");
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", contentType);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        return client().calculateRoute(POINTS);
    }

    private static void assertStraightLines(RouteCalculationResult result) {
        assertTrue(result.isEstimated());
        assertEquals(3, result.getGeometryPoints().size());
        assertEquals(2, result.getLegs().size());
        assertTrue(result.getTotalDistanceKm() > 0);
    }

    @Test
    void realRoute_followsTheRoads() {
        RouteCalculationResult result = answer(200, "application/json; charset=utf-8", ROUTE);

        assertFalse(result.isEstimated());
        assertEquals(5, result.getGeometryPoints().size());
        assertArrayEquals(new double[]{34.78, 10.70}, result.getGeometryPoints().get(1)); // OSRM is [lon, lat], we return [lat, lon]
        assertEquals(20.37, result.getLegs().get(0).getDistanceKm());
        assertEquals(32, result.getLegs().get(0).getDurationMinutes());
        assertEquals(35.73, result.getTotalDistanceKm());
        assertEquals(57, result.getTotalDurationMinutes());
        assertEquals("/route/v1/driving/10.761500,34.742000;10.620000,34.830000;10.700000,34.750000"
                + "?overview=full&geometries=geojson&steps=false", path);
    }

    // The real OSRM answers "deflate" in a form Java can't read
    @Test
    void compressionIsNotRequested() {
        answer(200, "application/json", ROUTE);
        assertNull(acceptEncoding);
    }

    @Test
    void serverError_fallsBackToStraightLines() {
        assertStraightLines(answer(503, "application/json", "{}"));
    }

    @Test
    void noRoute_fallsBackToStraightLines() {
        assertStraightLines(answer(200, "application/json", "{\"code\":\"NoRoute\",\"routes\":[]}"));
    }

    @Test
    void unreadableAnswer_fallsBackToStraightLines() {
        assertStraightLines(answer(200, "text/html", "<html>maintenance</html>"));
    }

    @Test
    void missingGeometryOrLegs_fallsBackToStraightLines() {
        assertStraightLines(answer(200, "application/json", "{\"code\":\"Ok\",\"routes\":[{\"legs\":[]}]}"));
    }

    @Test
    void tooFewLegs_fallsBackToStraightLines() {
        assertStraightLines(answer(200, "application/json", ROUTE.replace(",{\"distance\":15361.6,\"duration\":1490.9}", "")));
    }

    @Test
    void slowServer_fallsBackInsteadOfBlocking() {
        server.createContext("/", exchange -> {
            try {
                release.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            exchange.close();
        });
        assertTimeoutPreemptively(Duration.ofSeconds(4), () -> assertStraightLines(client().calculateRoute(POINTS)));
    }

    @Test
    void unreachableServer_fallsBackToStraightLines() {
        OsrmRouteClient client = client();
        server.stop(0);
        assertStraightLines(client.calculateRoute(POINTS));
    }

    @Test
    void singlePoint_needsNoRoute() {
        RouteCalculationResult result = client().calculateRoute(POINTS.subList(0, 1));
        assertFalse(result.isEstimated());
        assertEquals(1, result.getGeometryPoints().size());
    }
}
