package ISIMS.olive_connect.service;

import ISIMS.olive_connect.dto.AssignTourResourcesRequest;
import ISIMS.olive_connect.dto.AvailableTourResourcesResponse;
import ISIMS.olive_connect.dto.CancelTourRequest;
import ISIMS.olive_connect.dto.CollectionTourResponse;
import ISIMS.olive_connect.dto.CreateCollectionTourRequest;
import ISIMS.olive_connect.dto.EquipmentResponse;
import ISIMS.olive_connect.dto.UpdateTourDropOffsRequest;
import ISIMS.olive_connect.dto.UpdateTourStopsRequest;
import ISIMS.olive_connect.dto.WorkerResponse;
import ISIMS.olive_connect.entity.AccountStatus;
import ISIMS.olive_connect.entity.CollectionTour;
import ISIMS.olive_connect.entity.CollectionTourStatus;
import ISIMS.olive_connect.entity.Equipment;
import ISIMS.olive_connect.entity.EquipmentType;
import ISIMS.olive_connect.entity.HarvestRequest;
import ISIMS.olive_connect.entity.HarvestRequestStatus;
import ISIMS.olive_connect.entity.OilMill;
import ISIMS.olive_connect.entity.OilMillDropOff;
import ISIMS.olive_connect.entity.Orchard;
import ISIMS.olive_connect.entity.OrchardHarvestStatus;
import ISIMS.olive_connect.entity.Role;
import ISIMS.olive_connect.entity.TourStop;
import ISIMS.olive_connect.entity.User;
import ISIMS.olive_connect.entity.Worker;
import ISIMS.olive_connect.exception.ResourceNotFoundException;
import ISIMS.olive_connect.repository.CollectionTourRepository;
import ISIMS.olive_connect.repository.EquipmentRepository;
import ISIMS.olive_connect.repository.HarvestRequestRepository;
import ISIMS.olive_connect.repository.OrchardRepository;
import ISIMS.olive_connect.repository.UserRepository;
import ISIMS.olive_connect.repository.WorkerRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CollectionTourService {

    public static final List<CollectionTourStatus> CAPACITY_RESERVED_STATUSES = List.of(
            CollectionTourStatus.CONFIRMED,
            CollectionTourStatus.IN_PROGRESS,
            CollectionTourStatus.COMPLETED
    );

    public static final List<CollectionTourStatus> RESOURCE_LOCKED_STATUSES = List.of(
            CollectionTourStatus.CONFIRMED,
            CollectionTourStatus.IN_PROGRESS
    );

    private static final long DEFAULT_TOUR_WINDOW_HOURS = 8L;
    private static final int MIN_WORKERS_REQUIRED = 2;
    private static final double QUANTITY_EPSILON = 1e-6;
    private static final ZoneId TOUR_ZONE = ZoneId.of("Africa/Tunis");
    private static final LocalTime EARLIEST_START_TIME = LocalTime.of(7, 0);
    private static final LocalTime LATEST_END_TIME = LocalTime.of(17, 0);

    private final CollectionTourRepository tourRepository;
    private final HarvestRequestRepository harvestRequestRepository;
    private final OrchardRepository orchardRepository;
    private final UserRepository userRepository;
    private final OilMillService oilMillService;
    private final WorkerService workerService;
    private final WorkerAvailabilityService workerAvailabilityService;
    private final EquipmentService equipmentService;
    private final EquipmentAvailabilityService equipmentAvailabilityService;
    private final TourScheduleCalculationService scheduleCalculationService;
    private final WorkerRepository workerRepository;
    private final EquipmentRepository equipmentRepository;

    public CollectionTourService(CollectionTourRepository tourRepository,
                                 HarvestRequestRepository harvestRequestRepository,
                                 OrchardRepository orchardRepository,
                                 UserRepository userRepository,
                                 OilMillService oilMillService,
                                 WorkerService workerService,
                                 WorkerAvailabilityService workerAvailabilityService,
                                 EquipmentService equipmentService,
                                 EquipmentAvailabilityService equipmentAvailabilityService,
                                 TourScheduleCalculationService scheduleCalculationService,
                                 WorkerRepository workerRepository,
                                 EquipmentRepository equipmentRepository) {
        this.tourRepository = tourRepository;
        this.harvestRequestRepository = harvestRequestRepository;
        this.orchardRepository = orchardRepository;
        this.userRepository = userRepository;
        this.oilMillService = oilMillService;
        this.workerService = workerService;
        this.workerAvailabilityService = workerAvailabilityService;
        this.equipmentService = equipmentService;
        this.equipmentAvailabilityService = equipmentAvailabilityService;
        this.scheduleCalculationService = scheduleCalculationService;
        this.workerRepository = workerRepository;
        this.equipmentRepository = equipmentRepository;
    }

    public CollectionTourResponse createTour(CreateCollectionTourRequest request, String createdBy) {
        Instant now = Instant.now();
        String validatedTeamLeaderId = validateAndResolveTeamLeader(request.getTeamLeaderId());

        CollectionTour tour = new CollectionTour();
        tour.setTitle(request.getTitle().trim());
        tour.setNotes(request.getNotes() != null && !request.getNotes().isBlank() ? request.getNotes().trim() : null);
        tour.setTeamLeaderId(validatedTeamLeaderId);
        tour.setStatus(CollectionTourStatus.PLANNED);
        tour.setPlannedStartTime(request.getPlannedStartTime());
        tour.setCreatedBy(createdBy);
        tour.setCreatedAt(now);
        tour.setUpdatedAt(now);

        List<String> reqIds = request.getHarvestRequestIds() != null ? request.getHarvestRequestIds() : List.of();
        tour.setStops(buildOrderedStops(reqIds));

        List<CreateCollectionTourRequest.OilMillDropOffInput> dropOffInputs =
                request.getDropOffs() != null ? request.getDropOffs() : List.of();
        tour.setDropOffs(buildDropOffs(dropOffInputs, request.getPlannedStartTime(), null));

        List<String> workerIds = request.getWorkerIds() != null ? request.getWorkerIds() : List.of();
        List<String> equipmentIds = request.getEquipmentIds() != null ? request.getEquipmentIds() : List.of();
        validateNoDuplicates(workerIds, "worker");
        validateNoDuplicates(equipmentIds, "equipment");
        for (String workerId : workerIds) {
            workerService.findWorker(workerId);
        }
        for (String equipmentId : equipmentIds) {
            equipmentService.findEquipment(equipmentId);
        }
        validateResourcesForWindow(tour, workerIds, equipmentIds);
        tour.setAssignedWorkerIds(new ArrayList<>(workerIds));
        tour.setAssignedEquipmentIds(new ArrayList<>(equipmentIds));

        recomputeTotals(tour);
        scheduleCalculationService.recalculateTour(tour);
        validatePlannedTourConstraints(tour);

        return toResponse(tourRepository.save(tour));
    }

    public List<CollectionTourResponse> getTours(CollectionTourStatus status) {
        List<CollectionTour> tours = (status != null)
                ? tourRepository.findByStatusOrderByPlannedStartTimeAsc(status)
                : tourRepository.findAllByOrderByCreatedAtDesc();

        return tours.stream()
                .map(this::toResponse)
                .toList();
    }

    public CollectionTourResponse getTourById(String tourId) {
        return toResponse(findTour(tourId));
    }

    public CollectionTourResponse updateStops(String tourId, UpdateTourStopsRequest request) {
        CollectionTour tour = findTour(tourId);
        ensureEditable(tour);

        tour.setStops(buildOrderedStops(request.getOrderedHarvestRequestIds()));
        recomputeTotals(tour);
        scheduleCalculationService.recalculateTour(tour);
        validatePlannedTourConstraints(tour);
        tour.setUpdatedAt(Instant.now());

        return toResponse(tourRepository.save(tour));
    }

    public CollectionTourResponse updateDropOffs(String tourId, UpdateTourDropOffsRequest request) {
        CollectionTour tour = findTour(tourId);
        ensureEditable(tour);

        tour.setDropOffs(buildDropOffs(request.getDropOffs(), tour.getPlannedStartTime(), tour.getId()));
        recomputeTotals(tour);
        scheduleCalculationService.recalculateTour(tour);
        validatePlannedTourConstraints(tour);
        tour.setUpdatedAt(Instant.now());

        return toResponse(tourRepository.save(tour));
    }

    public CollectionTourResponse assignResources(String tourId, AssignTourResourcesRequest request) {
        CollectionTour tour = findTour(tourId);
        ensureEditable(tour);

        List<String> workerIds = request.getWorkerIds() != null ? request.getWorkerIds() : List.of();
        List<String> equipmentIds = request.getEquipmentIds() != null ? request.getEquipmentIds() : List.of();

        validateNoDuplicates(workerIds, "worker");
        validateNoDuplicates(equipmentIds, "equipment");

        for (String workerId : workerIds) {
            workerService.findWorker(workerId);
        }
        for (String equipmentId : equipmentIds) {
            equipmentService.findEquipment(equipmentId);
        }

        validateResourcesForWindow(tour, workerIds, equipmentIds);

        tour.setAssignedWorkerIds(new ArrayList<>(workerIds));
        tour.setAssignedEquipmentIds(new ArrayList<>(equipmentIds));
        recomputeTotals(tour);
        scheduleCalculationService.recalculateTour(tour);
        validatePlannedTourConstraints(tour);
        tour.setUpdatedAt(Instant.now());

        return toResponse(tourRepository.save(tour));
    }

    public CollectionTourResponse recalculateTour(String tourId) {
        CollectionTour tour = findTour(tourId);
        ensureEditable(tour);

        recomputeTotals(tour);
        scheduleCalculationService.recalculateTour(tour);
        validatePlannedTourConstraints(tour);
        tour.setUpdatedAt(Instant.now());

        return toResponse(tourRepository.save(tour));
    }

    // One confirmation at a time, so two tours can't both pass the resource check before either is saved.
    // shortcut: only guards a single backend instance, use a unique-index reservation if the app is scaled out
    public synchronized CollectionTourResponse confirmTour(String tourId) {
        CollectionTour tour = findTour(tourId);

        if (tour.getStatus() != CollectionTourStatus.PLANNED) {
            throw new IllegalStateException("Only PLANNED tours can be confirmed. Current status: " + tour.getStatus());
        }
        if (tour.getStops() == null || tour.getStops().isEmpty()) {
            throw new IllegalStateException("Cannot confirm a tour with no harvest stops.");
        }
        if (tour.getDropOffs() == null || tour.getDropOffs().isEmpty()) {
            throw new IllegalStateException("Cannot confirm a tour with no oil mill drop-offs.");
        }

        recomputeTotals(tour);
        scheduleCalculationService.recalculateTour(tour);
        validatePlannedTourConstraints(tour);

        List<CreateCollectionTourRequest.OilMillDropOffInput> dropOffInputs = tour.getDropOffs().stream()
                .map(d -> {
                    CreateCollectionTourRequest.OilMillDropOffInput input =
                            new CreateCollectionTourRequest.OilMillDropOffInput();
                    input.setOilMillId(d.getOilMillId());
                    input.setAllocatedQuantityKg(d.getAllocatedQuantityKg());
                    return input;
                })
                .toList();
        tour.setDropOffs(buildDropOffs(dropOffInputs, tour.getPlannedStartTime(), tour.getId()));

        validateTourResourcesAvailable(tour);

        recomputeTotals(tour);
        scheduleCalculationService.recalculateTour(tour);

        Instant now = Instant.now();
        tour.setStatus(CollectionTourStatus.CONFIRMED);
        tour.setConfirmedAt(now);
        tour.setUpdatedAt(now);

        CollectionTour saved = tourRepository.save(tour);
        syncStatusesOnTourConfirmed(saved);

        return toResponse(saved);
    }

    public CollectionTourResponse cancelTour(String tourId, CancelTourRequest request) {
        CollectionTour tour = findTour(tourId);

        if (tour.getStatus() == CollectionTourStatus.COMPLETED) {
            throw new IllegalStateException("Completed tours cannot be cancelled.");
        }
        if (tour.getStatus() == CollectionTourStatus.CANCELLED) {
            throw new IllegalStateException("Tour is already cancelled.");
        }

        Instant now = Instant.now();
        tour.setStatus(CollectionTourStatus.CANCELLED);
        tour.setCancelledAt(now);
        tour.setUpdatedAt(now);
        if (request != null && request.getReason() != null && !request.getReason().isBlank()) {
            tour.setCancellationReason(request.getReason().trim());
        }

        CollectionTour saved = tourRepository.save(tour);
        syncStatusesOnTourCancelled(saved);

        return toResponse(saved);
    }

    public AvailableTourResourcesResponse getAvailableResourcesForTour(String tourId) {
        CollectionTour tour = findTour(tourId);
        Instant[] window = resolveTourWindow(tour);
        Instant start = window[0];
        Instant end = window[1];

        AvailableTourResourcesResponse response = new AvailableTourResourcesResponse();
        response.setTourId(tour.getId());
        response.setWindowStart(start);
        response.setWindowEnd(end);
        response.setWorkers(getAvailableWorkers(start, end, tour.getId()));
        response.setEquipment(getAvailableEquipment(start, end, tour.getId()));
        return response;
    }

    // #26/#35: active workers with no unavailability period and no confirmed tour overlapping [start, end).
    // excludeTourId: the tour being edited, so its own assignments do not count as bookings (null = none).
    public List<WorkerResponse> getAvailableWorkers(Instant start, Instant end, String excludeTourId) {
        List<WorkerResponse> assignable = workerAvailabilityService.getAssignableWorkers(start, end);
        Set<String> booked = new HashSet<>();
        collectBookedResourcesFromOverlappingTours(excludeTourId, start, end, booked, new HashSet<>());
        return assignable.stream().filter(w -> !booked.contains(w.getId())).toList();
    }

    // #26/#36: same rule for equipment (maintenance, unavailability, confirmed tours)
    public List<EquipmentResponse> getAvailableEquipment(Instant start, Instant end, String excludeTourId) {
        List<EquipmentResponse> assignable = equipmentAvailabilityService.getAssignableEquipment(start, end);
        Set<String> booked = new HashSet<>();
        collectBookedResourcesFromOverlappingTours(excludeTourId, start, end, new HashSet<>(), booked);
        return assignable.stream().filter(e -> !booked.contains(e.getId())).toList();
    }

    public void validateTourResourcesAvailable(CollectionTour tour) {
        List<String> workerIds = tour.getAssignedWorkerIds() != null ? tour.getAssignedWorkerIds() : List.of();
        List<String> equipmentIds = tour.getAssignedEquipmentIds() != null ? tour.getAssignedEquipmentIds() : List.of();
        for (String workerId : workerIds) {
            workerService.findWorker(workerId);
        }
        for (String equipmentId : equipmentIds) {
            equipmentService.findEquipment(equipmentId);
        }
        validateResourcesForWindow(tour, workerIds, equipmentIds);
    }

    private String validateAndResolveTeamLeader(String rawTeamLeaderId) {
        if (rawTeamLeaderId == null || rawTeamLeaderId.isBlank()) {
            throw new IllegalArgumentException("A team leader (Chef d'équipe de récolte) must be assigned.");
        }
        String teamLeaderId = rawTeamLeaderId.trim();
        User teamLeader = userRepository.findById(teamLeaderId)
                .orElseThrow(() -> new ResourceNotFoundException("Team leader user not found with id: " + teamLeaderId));

        if (teamLeader.getRole() != Role.TEAM_LEADER) {
            throw new IllegalArgumentException("User '" + teamLeaderId + "' does not have role TEAM_LEADER.");
        }
        if (teamLeader.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Team leader '" + teamLeaderId + "' is not active (status: "
                    + teamLeader.getStatus() + ").");
        }
        return teamLeaderId;
    }

    private void validatePlannedTourConstraints(CollectionTour tour) {
        validateAndResolveTeamLeader(tour.getTeamLeaderId());

        validateDailyOperatingHours(tour);

        List<String> workerIds = tour.getAssignedWorkerIds() != null ? tour.getAssignedWorkerIds() : List.of();
        if (workerIds.size() < MIN_WORKERS_REQUIRED) {
            throw new IllegalArgumentException(
                    "A PLANNED tour must have at least " + MIN_WORKERS_REQUIRED + " seasonal workers assigned. Currently assigned: "
                            + workerIds.size());
        }

        List<String> equipmentIds = tour.getAssignedEquipmentIds() != null ? tour.getAssignedEquipmentIds() : List.of();
        List<Equipment> assignedEquipment = new ArrayList<>();
        for (String eqId : equipmentIds) {
            assignedEquipment.add(equipmentService.findEquipment(eqId));
        }

        double estimatedHarvestKg = tour.getTotalQuantityKg();
        long totalBinCapacityKg = assignedEquipment.stream()
                .filter(eq -> EquipmentType.BIN.name().equalsIgnoreCase(eq.getType()))
                .mapToLong(eq -> eq.getCapacityKg() != null ? eq.getCapacityKg() : 0L)
                .sum();

        if (totalBinCapacityKg < estimatedHarvestKg) {
            throw new IllegalArgumentException(
                    "Insufficient bin capacity (" + totalBinCapacityKg + " kg) for estimated harvest quantity ("
                            + estimatedHarvestKg + " kg).");
        }

        double totalAllocatedKg = tour.getTotalAllocatedQuantityKg();
        if (Math.abs(totalAllocatedKg - estimatedHarvestKg) > QUANTITY_EPSILON) {
            throw new IllegalArgumentException(
                    "Allocated oil mill drop-off quantity (" + totalAllocatedKg
                            + " kg) must equal the estimated harvest quantity ("
                            + estimatedHarvestKg + " kg).");
        }
    }

    private void validateDailyOperatingHours(CollectionTour tour) {
        if (tour.getPlannedStartTime() == null) {
            throw new IllegalArgumentException("Planned start time is required.");
        }
        ZonedDateTime startZdt = tour.getPlannedStartTime().atZone(TOUR_ZONE);
        if (startZdt.toLocalTime().isBefore(EARLIEST_START_TIME)) {
            throw new IllegalArgumentException(
                    "Tours cannot start before 07:00 (" + TOUR_ZONE + "). Requested start: " + startZdt);
        }

        Instant effectiveEnd = tour.getEstimatedEndTime() != null
                ? tour.getEstimatedEndTime()
                : tour.getPlannedStartTime();
        ZonedDateTime endZdt = effectiveEnd.atZone(TOUR_ZONE);
        ZonedDateTime sameDayLatestEnd = startZdt.toLocalDate().atTime(LATEST_END_TIME).atZone(TOUR_ZONE);

        if (!endZdt.toLocalDate().equals(startZdt.toLocalDate()) || endZdt.isAfter(sameDayLatestEnd)) {
            throw new IllegalArgumentException(
                    "Tours must finish on the same day before or at 17:00 (" + TOUR_ZONE
                            + "). Start: " + startZdt + ", Estimated end: " + endZdt);
        }
    }

    private void syncStatusesOnTourConfirmed(CollectionTour tour) {
        Set<String> requestIds = tour.getStops().stream()
                .map(TourStop::getHarvestRequestId)
                .collect(Collectors.toSet());
        Set<String> orchardIds = tour.getStops().stream()
                .map(TourStop::getOrchardId)
                .collect(Collectors.toSet());

        Instant now = Instant.now();

        List<HarvestRequest> requests = harvestRequestRepository.findAllById(requestIds);
        for (HarvestRequest hr : requests) {
            if (hr.getStatus() == HarvestRequestStatus.APPROVED) {
                hr.setStatus(HarvestRequestStatus.SCHEDULED);
                hr.setUpdatedAt(now);
            }
        }
        harvestRequestRepository.saveAll(requests);

        List<Orchard> orchards = orchardRepository.findAllById(orchardIds);
        for (Orchard orchard : orchards) {
            orchard.setHarvestStatus(OrchardHarvestStatus.SCHEDULED);
            orchard.setUpdatedAt(now);
        }
        orchardRepository.saveAll(orchards);
    }

    private void syncStatusesOnTourCancelled(CollectionTour cancelledTour) {
        Set<String> candidateRequestIds = cancelledTour.getStops().stream()
                .map(TourStop::getHarvestRequestId)
                .collect(Collectors.toSet());
        Set<String> candidateOrchardIds = cancelledTour.getStops().stream()
                .map(TourStop::getOrchardId)
                .collect(Collectors.toSet());

        List<CollectionTour> activeTours = tourRepository.findByStatusIn(RESOURCE_LOCKED_STATUSES);
        Set<String> stillLockedRequestIds = new HashSet<>();
        Set<String> stillLockedOrchardIds = new HashSet<>();
        for (CollectionTour active : activeTours) {
            if (active.getId().equals(cancelledTour.getId())) {
                continue;
            }
            for (TourStop s : active.getStops()) {
                stillLockedRequestIds.add(s.getHarvestRequestId());
                stillLockedOrchardIds.add(s.getOrchardId());
            }
        }

        Instant now = Instant.now();

        List<HarvestRequest> requestsToRestore = harvestRequestRepository.findAllById(candidateRequestIds).stream()
                .filter(hr -> !stillLockedRequestIds.contains(hr.getId()))
                .filter(hr -> hr.getStatus() == HarvestRequestStatus.SCHEDULED
                        || hr.getStatus() == HarvestRequestStatus.IN_PROGRESS)
                .toList();
        for (HarvestRequest hr : requestsToRestore) {
            hr.setStatus(HarvestRequestStatus.APPROVED);
            hr.setUpdatedAt(now);
        }
        harvestRequestRepository.saveAll(requestsToRestore);

        List<Orchard> orchardsToRestore = orchardRepository.findAllById(candidateOrchardIds).stream()
                .filter(o -> !stillLockedOrchardIds.contains(o.getId()))
                .filter(o -> o.getHarvestStatus() == OrchardHarvestStatus.SCHEDULED
                        || o.getHarvestStatus() == OrchardHarvestStatus.IN_PROGRESS)
                .toList();
        for (Orchard orchard : orchardsToRestore) {
            orchard.setHarvestStatus(OrchardHarvestStatus.REQUESTED);
            orchard.setUpdatedAt(now);
        }
        orchardRepository.saveAll(orchardsToRestore);
    }

    private void validateResourcesForWindow(CollectionTour tour, List<String> workerIds, List<String> equipmentIds) {
        Instant[] window = resolveTourWindow(tour);
        Instant start = window[0];
        Instant end = window[1];

        Set<String> assignableWorkerIds = workerAvailabilityService.getAssignableWorkers(start, end).stream()
                .map(WorkerResponse::getId)
                .collect(Collectors.toSet());

        for (String workerId : workerIds) {
            if (!assignableWorkerIds.contains(workerId)) {
                Worker worker = workerService.findWorker(workerId);
                throw new IllegalArgumentException("Worker '" + worker.getFullName()
                        + "' (" + workerId + ") is inactive or unavailable during [" + start + ", " + end + ").");
            }
        }

        Set<String> assignableEquipmentIds = equipmentAvailabilityService.getAssignableEquipment(start, end).stream()
                .map(EquipmentResponse::getId)
                .collect(Collectors.toSet());

        for (String equipmentId : equipmentIds) {
            if (!assignableEquipmentIds.contains(equipmentId)) {
                Equipment equipment = equipmentService.findEquipment(equipmentId);
                throw new IllegalArgumentException("Equipment '" + equipment.getReference()
                        + "' (" + equipmentId + ") is inactive, under maintenance, or unavailable during ["
                        + start + ", " + end + ").");
            }
        }

        Set<String> bookedWorkerIds = new HashSet<>();
        Set<String> bookedEquipmentIds = new HashSet<>();
        collectBookedResourcesFromOverlappingTours(tour.getId(), start, end, bookedWorkerIds, bookedEquipmentIds);

        for (String workerId : workerIds) {
            if (bookedWorkerIds.contains(workerId)) {
                throw new IllegalStateException("Worker " + workerId + " is already assigned to an overlapping tour.");
            }
        }
        for (String equipmentId : equipmentIds) {
            if (bookedEquipmentIds.contains(equipmentId)) {
                throw new IllegalStateException("Equipment " + equipmentId + " is already assigned to an overlapping tour.");
            }
        }
    }

    private void collectBookedResourcesFromOverlappingTours(String currentTourId,
                                                           Instant start,
                                                           Instant end,
                                                           Set<String> bookedWorkerIds,
                                                           Set<String> bookedEquipmentIds) {
        // Only confirmed / in-progress tours reserve resources (#42, #43): a draft never blocks another plan,
        // and the first tour to be confirmed wins (confirmTour re-checks).
        List<CollectionTour> overlappingTours = tourRepository.findByStatusIn(RESOURCE_LOCKED_STATUSES);
        for (CollectionTour other : overlappingTours) {
            if (currentTourId != null && currentTourId.equals(other.getId())) {
                continue;
            }
            Instant[] otherWindow = resolveTourWindow(other);
            Instant otherStart = otherWindow[0];
            Instant otherEnd = otherWindow[1];

            if (start.isBefore(otherEnd) && end.isAfter(otherStart)) {
                if (other.getAssignedWorkerIds() != null) {
                    bookedWorkerIds.addAll(other.getAssignedWorkerIds());
                }
                if (other.getAssignedEquipmentIds() != null) {
                    bookedEquipmentIds.addAll(other.getAssignedEquipmentIds());
                }
            }
        }
    }

    // Package-visible so the resource calendar (#27) uses the same tour window as conflict checks
    static Instant[] resolveTourWindow(CollectionTour tour) {
        Instant start = tour.getPlannedStartTime() != null ? tour.getPlannedStartTime() : Instant.now();
        Instant end = tour.getEstimatedEndTime() != null && tour.getEstimatedEndTime().isAfter(start)
                ? tour.getEstimatedEndTime()
                : start.plus(DEFAULT_TOUR_WINDOW_HOURS, ChronoUnit.HOURS);
        return new Instant[]{start, end};
    }

    private void validateNoDuplicates(List<String> ids, String resourceLabel) {
        Set<String> seen = new HashSet<>(ids);
        if (seen.size() != ids.size()) {
            throw new IllegalArgumentException("Duplicate " + resourceLabel + " IDs are not allowed in a tour.");
        }
    }

    // Assigned workers/equipment are returned with their details, so clients need no access to the logistics API
    private CollectionTourResponse toResponse(CollectionTour tour) {
        CollectionTourResponse response = CollectionTourResponse.fromEntity(tour);
        response.setAssignedWorkers(workerRepository.findAllById(tour.getAssignedWorkerIds()).stream()
                .map(WorkerResponse::fromEntity).toList());
        response.setAssignedEquipment(equipmentRepository.findAllById(tour.getAssignedEquipmentIds()).stream()
                .map(EquipmentResponse::fromEntity).toList());
        return response;
    }

    public CollectionTour findTour(String tourId) {
        return tourRepository.findById(tourId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection tour not found with id: " + tourId));
    }

    public void ensureEditable(CollectionTour tour) {
        if (tour.getStatus() != CollectionTourStatus.PLANNED) {
            throw new IllegalStateException("Only PLANNED tours can be modified. Current status: " + tour.getStatus());
        }
    }

    private List<TourStop> buildOrderedStops(List<String> orderedRequestIds) {
        if (orderedRequestIds.isEmpty()) {
            return new ArrayList<>();
        }

        Set<String> uniqueRequestIds = new HashSet<>(orderedRequestIds);
        if (uniqueRequestIds.size() != orderedRequestIds.size()) {
            throw new IllegalArgumentException("Duplicate harvest requests are not allowed in the same tour.");
        }

        Map<String, HarvestRequest> requestMap = harvestRequestRepository.findAllById(uniqueRequestIds)
                .stream()
                .collect(Collectors.toMap(HarvestRequest::getId, Function.identity()));

        List<HarvestRequest> orderedRequests = new ArrayList<>();
        Set<String> orchardIds = new HashSet<>();
        for (String reqId : orderedRequestIds) {
            HarvestRequest hr = requestMap.get(reqId);
            if (hr == null) {
                throw new ResourceNotFoundException("Harvest request not found with id: " + reqId);
            }
            if (hr.getStatus() != HarvestRequestStatus.APPROVED) {
                throw new IllegalArgumentException("Only APPROVED harvest requests can be added to a tour. Request "
                        + reqId + " has status: " + hr.getStatus());
            }
            if (!orchardIds.add(hr.getOrchardId())) {
                throw new IllegalArgumentException("Duplicate orchard in the same tour is not allowed: " + hr.getOrchardId());
            }
            orderedRequests.add(hr);
        }

        Map<String, Orchard> orchardMap = orchardRepository.findAllById(orchardIds)
                .stream()
                .collect(Collectors.toMap(Orchard::getId, Function.identity()));

        List<TourStop> stops = new ArrayList<>();
        for (int i = 0; i < orderedRequests.size(); i++) {
            HarvestRequest hr = orderedRequests.get(i);
            Orchard orchard = orchardMap.get(hr.getOrchardId());
            if (orchard == null) {
                throw new ResourceNotFoundException("Orchard not found with id: " + hr.getOrchardId());
            }

            TourStop stop = new TourStop();
            stop.setVisitOrder(i + 1);
            stop.setHarvestRequestId(hr.getId());
            stop.setOrchardId(orchard.getId());
            stop.setEstimatedQuantityKg(hr.getEstimatedQuantityKg() != null ? hr.getEstimatedQuantityKg() : 0.0);
            stops.add(stop);
        }
        return stops;
    }

    public List<OilMillDropOff> buildDropOffs(List<CreateCollectionTourRequest.OilMillDropOffInput> inputs,
                                              Instant plannedStartTime,
                                              String excludeTourId) {
        if (inputs.isEmpty()) {
            return new ArrayList<>();
        }

        LocalDate tourDay = (plannedStartTime != null ? plannedStartTime : Instant.now())
                .atZone(ZoneOffset.UTC)
                .toLocalDate();
        Instant dayStart = tourDay.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant dayEnd = tourDay.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<CollectionTour> activeCapacityTours = tourRepository.findByStatusIn(CAPACITY_RESERVED_STATUSES);

        Map<String, Double> requestedPerMill = new HashMap<>();
        for (CreateCollectionTourRequest.OilMillDropOffInput input : inputs) {
            requestedPerMill.merge(input.getOilMillId(), input.getAllocatedQuantityKg(), Double::sum);
        }

        for (Map.Entry<String, Double> entry : requestedPerMill.entrySet()) {
            String millId = entry.getKey();
            double totalRequestedForMill = entry.getValue();
            OilMill mill = oilMillService.findOilMill(millId);
            if (!mill.isActive()) {
                throw new IllegalArgumentException("Oil mill is inactive: " + mill.getName());
            }

            double alreadyBookedKg = activeCapacityTours.stream()
                    .filter(t -> excludeTourId == null || !excludeTourId.equals(t.getId()))
                    .filter(t -> t.getPlannedStartTime() != null
                            && !t.getPlannedStartTime().isBefore(dayStart)
                            && t.getPlannedStartTime().isBefore(dayEnd))
                    .flatMap(t -> t.getDropOffs().stream())
                    .filter(d -> mill.getId().equals(d.getOilMillId()))
                    .mapToDouble(OilMillDropOff::getAllocatedQuantityKg)
                    .sum();

            if (alreadyBookedKg + totalRequestedForMill > mill.getDailyCapacityKg()) {
                throw new IllegalArgumentException("Daily capacity exceeded for oil mill '" + mill.getName()
                        + "' on " + tourDay + ": requested " + totalRequestedForMill
                        + " kg + already booked " + alreadyBookedKg + " kg > capacity "
                        + mill.getDailyCapacityKg() + " kg");
            }
        }

        List<OilMillDropOff> dropOffs = new ArrayList<>();
        for (CreateCollectionTourRequest.OilMillDropOffInput input : inputs) {
            OilMillDropOff dropOff = new OilMillDropOff();
            dropOff.setOilMillId(input.getOilMillId());
            dropOff.setAllocatedQuantityKg(input.getAllocatedQuantityKg());
            dropOffs.add(dropOff);
        }
        return dropOffs;
    }

    public void recomputeTotals(CollectionTour tour) {
        double totalHarvestKg = tour.getStops().stream()
                .mapToDouble(TourStop::getEstimatedQuantityKg)
                .sum();
        double totalAllocatedKg = tour.getDropOffs().stream()
                .mapToDouble(OilMillDropOff::getAllocatedQuantityKg)
                .sum();

        tour.setTotalQuantityKg(totalHarvestKg);
        tour.setTotalAllocatedQuantityKg(totalAllocatedKg);
    }
}