package ISIMS.olive_connect.service;

import ISIMS.olive_connect.entity.CollectionTour;
import ISIMS.olive_connect.entity.OilMill;
import ISIMS.olive_connect.entity.OilMillDropOff;
import ISIMS.olive_connect.entity.Orchard;
import ISIMS.olive_connect.entity.TourStop;
import ISIMS.olive_connect.exception.ResourceNotFoundException;
import ISIMS.olive_connect.repository.OrchardRepository;
import ISIMS.olive_connect.service.routing.GeoPoint;
import ISIMS.olive_connect.service.routing.HarvestingDurationEstimator;
import ISIMS.olive_connect.service.routing.OsrmRouteClient;
import ISIMS.olive_connect.service.routing.RouteCalculationResult;
import ISIMS.olive_connect.service.routing.RouteLeg;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TourScheduleCalculationService {

    private static final long UNLOADING_BUFFER_MINUTES = 15L;

    private final OrchardRepository orchardRepository;
    private final OilMillService oilMillService;
    private final OsrmRouteClient routeClient;
    private final HarvestingDurationEstimator durationEstimator;

    public TourScheduleCalculationService(OrchardRepository orchardRepository,
                                          OilMillService oilMillService,
                                          OsrmRouteClient routeClient,
                                          HarvestingDurationEstimator durationEstimator) {
        this.orchardRepository = orchardRepository;
        this.oilMillService = oilMillService;
        this.routeClient = routeClient;
        this.durationEstimator = durationEstimator;
    }

    public void recalculateTour(CollectionTour tour) {
        List<TourStop> stops = tour.getStops() != null ? tour.getStops() : List.of();
        List<OilMillDropOff> dropOffs = tour.getDropOffs() != null ? tour.getDropOffs() : List.of();

        Set<String> orchardIds = stops.stream()
                .map(TourStop::getOrchardId)
                .collect(Collectors.toSet());
        Map<String, Orchard> orchardMap = orchardRepository.findAllById(orchardIds)
                .stream()
                .collect(Collectors.toMap(Orchard::getId, o -> o));

        int workerCount = tour.getAssignedWorkerIds() != null ? tour.getAssignedWorkerIds().size() : 0;

        // Build ordered waypoints: [Orchard 1, ..., Orchard N, OilMill 1, ..., OilMill M]
        List<GeoPoint> waypoints = new ArrayList<>();
        for (TourStop stop : stops) {
            Orchard orchard = orchardMap.get(stop.getOrchardId());
            if (orchard == null) {
                throw new ResourceNotFoundException("Orchard not found with id: " + stop.getOrchardId());
            }

            GeoJsonPoint location = orchard.getLocation();
            if (location == null) {
                throw new IllegalStateException("Orchard '" + orchard.getName()
                        + "' (" + orchard.getId() + ") does not have location coordinates set.");
            }

            double lat = location.getY();
            double lon = location.getX();

            stop.setOrchardName(orchard.getName());
            stop.setLatitude(lat);
            stop.setLongitude(lon);
            waypoints.add(new GeoPoint(lat, lon));
        }

        // One trip for every mill of the tour
        Map<String, OilMill> mills = dropOffs.isEmpty() ? Map.of()
                : oilMillService.findOilMills(dropOffs.stream().map(OilMillDropOff::getOilMillId).distinct().toList());
        for (OilMillDropOff dropOff : dropOffs) {
            OilMill mill = mills.get(dropOff.getOilMillId());
            dropOff.setOilMillName(mill.getName());
            dropOff.setLatitude(mill.getLatitude());
            dropOff.setLongitude(mill.getLongitude());
            waypoints.add(new GeoPoint(mill.getLatitude(), mill.getLongitude()));
        }

        RouteCalculationResult route = routeClient.calculateRoute(waypoints);
        tour.setRouteEstimated(route.isEstimated());
        tour.setRouteGeometry(toGeometryList(route.getGeometryPoints()));

        List<RouteLeg> legs = route.getLegs();
        int legIndex = 0;

        Instant baseStart = tour.getPlannedStartTime() != null ? tour.getPlannedStartTime() : Instant.now();
        Instant currentTime = baseStart;

        long totalTravelMinutes = 0L;
        long totalHarvestingMinutes = 0L;
        double totalDistanceKm = 0.0;

        // 1. Orchard stops schedule (harvest duration calculated strictly from quantity and workerCount)
        for (int i = 0; i < stops.size(); i++) {
            TourStop stop = stops.get(i);
            stop.setVisitOrder(i + 1);

            long travelMin = 0L;
            double travelKm = 0.0;
            if (i > 0 && legIndex < legs.size()) {
                RouteLeg leg = legs.get(legIndex++);
                travelMin = leg.getDurationMinutes();
                travelKm = leg.getDistanceKm();
            }

            stop.setTravelDistanceKmFromPrevious(travelKm);
            stop.setTravelDurationMinutesFromPrevious(travelMin);
            totalTravelMinutes += travelMin;
            totalDistanceKm += travelKm;

            Instant arrival = currentTime.plus(travelMin, ChronoUnit.MINUTES);
            long harvestMin = durationEstimator.estimateMinutes(
                    stop.getEstimatedQuantityKg(), workerCount, List.of());
            Instant departure = arrival.plus(harvestMin, ChronoUnit.MINUTES);

            stop.setEstimatedArrival(arrival);
            stop.setHarvestingDurationMinutes(harvestMin);
            stop.setEstimatedDeparture(departure);

            totalHarvestingMinutes += harvestMin;
            currentTime = departure;
        }

        // 2. Oil mill drop-offs arrival estimation
        for (int j = 0; j < dropOffs.size(); j++) {
            OilMillDropOff dropOff = dropOffs.get(j);
            long travelMin = 0L;
            double travelKm = 0.0;

            boolean hasPreviousPoint = (stops.size() + j) > 0;
            if (hasPreviousPoint && legIndex < legs.size()) {
                RouteLeg leg = legs.get(legIndex++);
                travelMin = leg.getDurationMinutes();
                travelKm = leg.getDistanceKm();
            }

            dropOff.setTravelDistanceKmFromPrevious(travelKm);
            dropOff.setTravelDurationMinutesFromPrevious(travelMin);
            totalTravelMinutes += travelMin;
            totalDistanceKm += travelKm;

            Instant millArrival = currentTime.plus(travelMin, ChronoUnit.MINUTES);
            dropOff.setEstimatedArrival(millArrival);

            currentTime = millArrival.plus(UNLOADING_BUFFER_MINUTES, ChronoUnit.MINUTES);
        }

        long totalTourMinutes = totalTravelMinutes + totalHarvestingMinutes;

        tour.setTotalDistanceKm(Math.round(totalDistanceKm * 100.0) / 100.0);
        tour.setTotalTravelDurationMinutes(totalTravelMinutes);
        tour.setTotalHarvestingDurationMinutes(totalHarvestingMinutes);
        tour.setTotalTourDurationMinutes(totalTourMinutes);
        tour.setEstimatedEndTime(baseStart.plus(totalTourMinutes, ChronoUnit.MINUTES));
    }

    private List<List<Double>> toGeometryList(List<double[]> rawPoints) {
        if (rawPoints == null || rawPoints.isEmpty()) {
            return List.of();
        }
        List<List<Double>> converted = new ArrayList<>(rawPoints.size());
        for (double[] pt : rawPoints) {
            if (pt != null && pt.length >= 2) {
                converted.add(List.of(pt[0], pt[1]));
            }
        }
        return converted;
    }
}