package ISIMS.olive_connect.service;

import ISIMS.olive_connect.dto.AssignTourResourcesRequest;
import ISIMS.olive_connect.dto.AvailableTourResourcesResponse;
import ISIMS.olive_connect.dto.CancelTourRequest;
import ISIMS.olive_connect.dto.CollectionTourResponse;
import ISIMS.olive_connect.dto.CreateCollectionTourRequest;
import ISIMS.olive_connect.dto.EquipmentResponse;
import ISIMS.olive_connect.dto.EquipmentStatusResponse;
import ISIMS.olive_connect.dto.TeamLeaderResponse;
import ISIMS.olive_connect.dto.UpdateTourDropOffsRequest;
import ISIMS.olive_connect.dto.UpdateTourStopsRequest;
import ISIMS.olive_connect.dto.UpdateTourTeamLeaderRequest;
import ISIMS.olive_connect.dto.WorkerResponse;
import ISIMS.olive_connect.entity.AccountStatus;
import ISIMS.olive_connect.entity.CollectionTour;
import ISIMS.olive_connect.entity.CollectionTourStatus;
import ISIMS.olive_connect.entity.Equipment;
import ISIMS.olive_connect.entity.EquipmentStatus;
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
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        loadWorkers(workerIds);
        loadEquipment(equipmentIds);
        validateResourcesForWindow(tour, workerIds, equipmentIds);
        tour.setAssignedWorkerIds(new ArrayList<>(workerIds));
        tour.setAssignedEquipmentIds(new ArrayList<>(equipmentIds));

        recomputeTotals(tour);
        scheduleCalculationService.recalculateTour(tour);
        validatePlannedTourConstraints(tour);
        validateAllocationMatchesHarvest(tour);
        validateTeamLeaderAvailable(tour);

        return toResponse(tourRepository.save(tour));
    }

    public List<CollectionTourResponse> getTours(CollectionTourStatus status) {
        List<CollectionTour> tours = (status != null)
                ? tourRepository.findByStatusOrderByPlannedStartTimeAsc(status)
                : tourRepository.findAllByOrderByCreatedAtDesc();

        return toResponses(tours);
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

        loadWorkers(workerIds);
        loadEquipment(equipmentIds);

        validateResourcesForWindow(tour, workerIds, equipmentIds);

        tour.setAssignedWorkerIds(new ArrayList<>(workerIds));
        tour.setAssignedEquipmentIds(new ArrayList<>(equipmentIds));
        recomputeTotals(tour);
        scheduleCalculationService.recalculateTour(tour);
        validatePlannedTourConstraints(tour);
        tour.setUpdatedAt(Instant.now());

        return toResponse(tourRepository.save(tour));
    }

    // Synchronized with confirmTour, so saving a stale draft can't undo a confirmation
    public synchronized CollectionTourResponse updateTeamLeader(String tourId, UpdateTourTeamLeaderRequest request) {
        CollectionTour tour = findTour(tourId);
        ensureEditable(tour);

        tour.setTeamLeaderId(validateAndResolveTeamLeader(request.getTeamLeaderId()));
        validateTeamLeaderAvailable(tour);
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
        // Read once: this method is synchronized, so the tours confirmed here don't change meanwhile
        List<CollectionTour> locked = tourRepository.findByStatusIn(RESOURCE_LOCKED_STATUSES);
        validateOrchardsNotInAnotherTour(tour, locked);
        validateRequestsStillApproved(tour);

        recomputeTotals(tour);
        scheduleCalculationService.recalculateTour(tour);
        validatePlannedTourConstraints(tour);
        validateAllocationMatchesHarvest(tour);

        // The drop-offs are kept: rebuilding them erased what recalculateTour filled in and forced a second route
        checkMillCapacity(tour.getDropOffs().stream().collect(Collectors.toMap(
                        OilMillDropOff::getOilMillId, OilMillDropOff::getAllocatedQuantityKg, Double::sum)),
                tour.getPlannedStartTime(), tour.getId());

        validateTourResourcesAvailable(tour, locked);
        validateTeamLeaderAvailable(tour, locked);

        Instant now = Instant.now();
        tour.setStatus(CollectionTourStatus.CONFIRMED);
        tour.setConfirmedAt(now);
        tour.setUpdatedAt(now);

        CollectionTour saved = tourRepository.save(tour);
        syncStatusesOnTourConfirmed(saved);

        return toResponse(saved);
    }

    // Only a draft can be removed: it reserved nothing, while other tours are kept as history.
    // Synchronized with confirmTour, so a tour being confirmed can't be deleted
    public synchronized void deleteDraft(String tourId) {
        CollectionTour tour = findTour(tourId);
        if (tour.getStatus() != CollectionTourStatus.PLANNED) {
            throw new IllegalStateException("Only PLANNED tours can be deleted. Current status: " + tour.getStatus());
        }
        tourRepository.delete(tour);
    }

    public CollectionTourResponse cancelTour(String tourId, CancelTourRequest request) {
        CollectionTour tour = findTour(tourId);

        if (tour.getStatus() == CollectionTourStatus.PLANNED) {
            throw new IllegalStateException("Only confirmed tours can be cancelled. Delete the draft instead.");
        }
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

    // Active team leaders with no confirmed tour overlapping [start, end), sorted by name
    public List<TeamLeaderResponse> getAvailableTeamLeaders(Instant start, Instant end) {
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Start time must be before end time.");
        }
        Set<String> booked = findOverlappingTours(null, start, end).stream()
                .map(CollectionTour::getTeamLeaderId)
                .collect(Collectors.toSet());
        return userRepository.findByRoleAndStatus(Role.TEAM_LEADER, AccountStatus.ACTIVE).stream()
                .filter(user -> !booked.contains(user.getId()))
                .map(TeamLeaderResponse::fromEntity)
                .sorted(Comparator.comparing(TeamLeaderResponse::getFullName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    // #25: current equipment status. The logistics status (maintenance > unavailable > available) is kept,
    // and AVAILABLE becomes IN_USE while a confirmed tour using the equipment is running.
    public EquipmentStatusResponse getEquipmentStatus(String equipmentId) {
        EquipmentStatusResponse status = equipmentAvailabilityService.getCurrentStatus(equipmentId);
        if (status.getStatus() == EquipmentStatus.AVAILABLE && isInRunningTour(equipmentId, Instant.now())) {
            status.setStatus(EquipmentStatus.IN_USE);
        }
        return status;
    }

    // True when a confirmed / in-progress tour assigned this equipment and its window [start, end) contains `at`
    boolean isInRunningTour(String equipmentId, Instant at) {
        return tourRepository.findByStatusIn(RESOURCE_LOCKED_STATUSES).stream()
                .filter(tour -> tour.getAssignedEquipmentIds() != null && tour.getAssignedEquipmentIds().contains(equipmentId))
                .map(CollectionTourService::resolveTourWindow)
                .anyMatch(window -> !at.isBefore(window[0]) && at.isBefore(window[1]));
    }

    public void validateTourResourcesAvailable(CollectionTour tour) {
        validateTourResourcesAvailable(tour, tourRepository.findByStatusIn(RESOURCE_LOCKED_STATUSES));
    }

    // locked: the confirmed / in-progress tours, already read by the caller
    private void validateTourResourcesAvailable(CollectionTour tour, List<CollectionTour> locked) {
        List<String> workerIds = tour.getAssignedWorkerIds() != null ? tour.getAssignedWorkerIds() : List.of();
        List<String> equipmentIds = tour.getAssignedEquipmentIds() != null ? tour.getAssignedEquipmentIds() : List.of();
        loadWorkers(workerIds);
        loadEquipment(equipmentIds);
        validateResourcesForWindow(tour, workerIds, equipmentIds, locked);
    }

    // One trip for all ids; an unknown id is refused with WorkerService.findWorker's message
    private Map<String, Worker> loadWorkers(List<String> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<String, Worker> found = workerRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Worker::getId, Function.identity()));
        for (String id : ids) {
            if (!found.containsKey(id)) {
                throw new ResourceNotFoundException("Worker not found with id: " + id);
            }
        }
        return found;
    }

    // One trip for all ids; an unknown id is refused with EquipmentService.findEquipment's message
    private Map<String, Equipment> loadEquipment(List<String> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<String, Equipment> found = equipmentRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Equipment::getId, Function.identity()));
        for (String id : ids) {
            if (!found.containsKey(id)) {
                throw new ResourceNotFoundException("Equipment not found with id: " + id);
            }
        }
        return found;
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

    // An orchard can be in only one confirmed / in-progress tour at a time, whatever the dates
    private void validateOrchardsNotInAnotherTour(CollectionTour tour, List<CollectionTour> locked) {
        Set<String> orchardIds = tour.getStops().stream().map(TourStop::getOrchardId).collect(Collectors.toSet());
        for (CollectionTour other : locked) {
            for (TourStop stop : other.getStops()) {
                if (orchardIds.contains(stop.getOrchardId())) {
                    throw new IllegalStateException("Cannot confirm: orchard '" + orchardName(stop.getOrchardId())
                            + "' is already in the confirmed tour '" + other.getTitle() + "'.");
                }
            }
        }
    }

    // Requests were approved when added to the draft, but may have changed since
    private void validateRequestsStillApproved(CollectionTour tour) {
        Map<String, HarvestRequest> requests = harvestRequestRepository
                .findAllById(tour.getStops().stream().map(TourStop::getHarvestRequestId).toList()).stream()
                .collect(Collectors.toMap(HarvestRequest::getId, Function.identity()));
        for (TourStop stop : tour.getStops()) {
            HarvestRequest request = requests.get(stop.getHarvestRequestId());
            if (request == null) {
                throw new IllegalStateException("Cannot confirm: harvest request " + stop.getHarvestRequestId()
                        + " no longer exists.");
            }
            if (request.getStatus() != HarvestRequestStatus.APPROVED) {
                throw new IllegalStateException("Cannot confirm: the harvest request for orchard '"
                        + orchardName(stop.getOrchardId()) + "' is " + request.getStatus() + ", not APPROVED.");
            }
        }
    }

    private String orchardName(String orchardId) {
        return orchardRepository.findById(orchardId).map(Orchard::getName).orElse(orchardId);
    }

    // Not part of validatePlannedTourConstraints: a draft whose leader became busy must stay editable
    private void validateTeamLeaderAvailable(CollectionTour tour) {
        validateTeamLeaderAvailable(tour, tourRepository.findByStatusIn(RESOURCE_LOCKED_STATUSES));
    }

    private void validateTeamLeaderAvailable(CollectionTour tour, List<CollectionTour> locked) {
        Instant[] window = resolveTourWindow(tour);
        boolean busy = findOverlappingTours(tour.getId(), window[0], window[1], locked).stream()
                .anyMatch(other -> tour.getTeamLeaderId().equals(other.getTeamLeaderId()));
        if (busy) {
            String name = userRepository.findById(tour.getTeamLeaderId())
                    .map(User::getFullName)
                    .orElse(tour.getTeamLeaderId());
            throw new IllegalStateException("Team leader '" + name + "' is already assigned to an overlapping tour.");
        }
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
        Collection<Equipment> assignedEquipment = loadEquipment(equipmentIds).values();

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
    }

    // Not checked on draft edits: stops and drop-offs are saved by two calls, so a draft is unequal between them
    private void validateAllocationMatchesHarvest(CollectionTour tour) {
        double estimatedHarvestKg = tour.getTotalQuantityKg();
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
        validateResourcesForWindow(tour, workerIds, equipmentIds, tourRepository.findByStatusIn(RESOURCE_LOCKED_STATUSES));
    }

    private void validateResourcesForWindow(CollectionTour tour, List<String> workerIds, List<String> equipmentIds,
                                            List<CollectionTour> locked) {
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
        for (CollectionTour other : findOverlappingTours(tour.getId(), start, end, locked)) {
            bookedWorkerIds.addAll(orEmpty(other.getAssignedWorkerIds()));
            bookedEquipmentIds.addAll(orEmpty(other.getAssignedEquipmentIds()));
        }

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
        for (CollectionTour other : findOverlappingTours(currentTourId, start, end)) {
            if (other.getAssignedWorkerIds() != null) {
                bookedWorkerIds.addAll(other.getAssignedWorkerIds());
            }
            if (other.getAssignedEquipmentIds() != null) {
                bookedEquipmentIds.addAll(other.getAssignedEquipmentIds());
            }
        }
    }

    // Only confirmed / in-progress tours reserve resources (#42, #43): a draft never blocks another plan,
    // and the first tour to be confirmed wins (confirmTour re-checks).
    private List<CollectionTour> findOverlappingTours(String excludeTourId, Instant start, Instant end) {
        return findOverlappingTours(excludeTourId, start, end, tourRepository.findByStatusIn(RESOURCE_LOCKED_STATUSES));
    }

    private static List<CollectionTour> findOverlappingTours(String excludeTourId, Instant start, Instant end,
                                                            List<CollectionTour> locked) {
        return locked.stream()
                .filter(other -> excludeTourId == null || !excludeTourId.equals(other.getId()))
                .filter(other -> {
                    Instant[] window = resolveTourWindow(other);
                    return start.isBefore(window[1]) && end.isAfter(window[0]);
                })
                .toList();
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
        return toResponses(List.of(tour)).get(0);
    }

    // Leaders, workers and equipment are each loaded once for all the tours: a query per tour made lists take seconds
    private List<CollectionTourResponse> toResponses(List<CollectionTour> tours) {
        Map<String, User> leaders = loadById(userRepository, tours.stream().map(CollectionTour::getTeamLeaderId), User::getId);
        Map<String, Worker> workers = loadById(workerRepository,
                tours.stream().flatMap(tour -> orEmpty(tour.getAssignedWorkerIds()).stream()), Worker::getId);
        Map<String, Equipment> equipment = loadById(equipmentRepository,
                tours.stream().flatMap(tour -> orEmpty(tour.getAssignedEquipmentIds()).stream()), Equipment::getId);

        return tours.stream().map(tour -> {
            CollectionTourResponse response = CollectionTourResponse.fromEntity(tour);
            User leader = tour.getTeamLeaderId() != null ? leaders.get(tour.getTeamLeaderId()) : null;
            response.setTeamLeaderName(leader != null ? leader.getFullName() : null);
            response.setAssignedWorkers(orEmpty(tour.getAssignedWorkerIds()).stream()
                    .map(workers::get).filter(Objects::nonNull).map(WorkerResponse::fromEntity).toList());
            response.setAssignedEquipment(orEmpty(tour.getAssignedEquipmentIds()).stream()
                    .map(equipment::get).filter(Objects::nonNull).map(EquipmentResponse::fromEntity).toList());
            return response;
        }).toList();
    }

    private static <T> Map<String, T> loadById(MongoRepository<T, String> repository, Stream<String> ids, Function<T, String> id) {
        List<String> wanted = ids.filter(Objects::nonNull).distinct().toList();
        return wanted.isEmpty() ? Map.of()
                : repository.findAllById(wanted).stream().collect(Collectors.toMap(id, Function.identity()));
    }

    private static List<String> orEmpty(List<String> ids) {
        return ids != null ? ids : List.of();
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

        Map<String, Double> requestedPerMill = new HashMap<>();
        for (CreateCollectionTourRequest.OilMillDropOffInput input : inputs) {
            requestedPerMill.merge(input.getOilMillId(), input.getAllocatedQuantityKg(), Double::sum);
        }
        checkMillCapacity(requestedPerMill, plannedStartTime, excludeTourId);

        List<OilMillDropOff> dropOffs = new ArrayList<>();
        for (CreateCollectionTourRequest.OilMillDropOffInput input : inputs) {
            OilMillDropOff dropOff = new OilMillDropOff();
            dropOff.setOilMillId(input.getOilMillId());
            dropOff.setAllocatedQuantityKg(input.getAllocatedQuantityKg());
            dropOffs.add(dropOff);
        }
        return dropOffs;
    }

    // Each mill must be active and keep enough daily capacity. requestedPerMill: { millId: kg }
    private void checkMillCapacity(Map<String, Double> requestedPerMill, Instant plannedStartTime, String excludeTourId) {
        LocalDate tourDay = (plannedStartTime != null ? plannedStartTime : Instant.now())
                .atZone(ZoneOffset.UTC)
                .toLocalDate();
        Instant dayStart = tourDay.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant dayEnd = tourDay.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<CollectionTour> activeCapacityTours = tourRepository.findByStatusIn(CAPACITY_RESERVED_STATUSES);
        Map<String, OilMill> mills = oilMillService.findOilMills(requestedPerMill.keySet());

        for (Map.Entry<String, Double> entry : requestedPerMill.entrySet()) {
            String millId = entry.getKey();
            double totalRequestedForMill = entry.getValue();
            OilMill mill = mills.get(millId);
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