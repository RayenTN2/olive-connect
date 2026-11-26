package ISIMS.olive_connect.service;

import ISIMS.olive_connect.dto.AssignTourResourcesRequest;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CollectionTourServiceTest {

    // 08:00 → 13:00 in Tunis (UTC+1)
    private static final Instant START = Instant.parse("2026-11-25T07:00:00Z");
    private static final Instant END = Instant.parse("2026-11-25T12:00:00Z");

    @Mock private CollectionTourRepository tourRepository;
    @Mock private HarvestRequestRepository harvestRequestRepository;
    @Mock private OrchardRepository orchardRepository;
    @Mock private UserRepository userRepository;
    @Mock private OilMillService oilMillService;
    @Mock private WorkerService workerService;
    @Mock private WorkerAvailabilityService workerAvailabilityService;
    @Mock private EquipmentService equipmentService;
    @Mock private EquipmentAvailabilityService equipmentAvailabilityService;
    @Mock private TourScheduleCalculationService scheduleCalculationService;
    @Mock private WorkerRepository workerRepository;
    @Mock private EquipmentRepository equipmentRepository;

    @InjectMocks
    private CollectionTourService service;

    @BeforeEach
    void setUp() {
        lenient().when(tourRepository.save(any(CollectionTour.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static WorkerResponse workerResponse(String id) {
        WorkerResponse response = new WorkerResponse();
        response.setId(id);
        return response;
    }

    private static Worker worker(String id) {
        Worker worker = new Worker();
        worker.setId(id);
        worker.setFullName("Worker " + id);
        worker.setActive(true);
        return worker;
    }

    private static Equipment bin(String id) {
        Equipment equipment = new Equipment();
        equipment.setId(id);
        equipment.setReference("BN-" + id);
        equipment.setType("BIN");
        equipment.setCapacityKg(500);
        equipment.setActive(true);
        return equipment;
    }

    // The service reads workers, equipment and mills by groups: each id asked for exists
    private static <T> List<T> each(Object ids, java.util.function.Function<String, T> make) {
        List<T> found = new java.util.ArrayList<>();
        for (Object id : (Iterable<?>) ids) found.add(make.apply((String) id));
        return found;
    }

    private void workersExist() {
        lenient().doAnswer(inv -> each(inv.getArgument(0), CollectionTourServiceTest::worker)).when(workerRepository).findAllById(any());
    }

    private void binsExist() {
        lenient().doAnswer(inv -> each(inv.getArgument(0), CollectionTourServiceTest::bin)).when(equipmentRepository).findAllById(any());
    }

    private static CollectionTour tour(String id, CollectionTourStatus status, String start, String end,
                                       List<String> workerIds, List<String> equipmentIds) {
        CollectionTour tour = new CollectionTour();
        tour.setId(id);
        tour.setStatus(status);
        tour.setTeamLeaderId("tl");
        tour.setPlannedStartTime(Instant.parse(start));
        tour.setEstimatedEndTime(end != null ? Instant.parse(end) : null);
        tour.setAssignedWorkerIds(workerIds);
        tour.setAssignedEquipmentIds(equipmentIds);
        return tour;
    }

    // --- tour-aware availability (#26, #35, #36, #42) ---

    @Test
    void availableWorkers_excludeOnlyResourcesOfOverlappingConfirmedTours() {
        when(workerAvailabilityService.getAssignableWorkers(START, END))
                .thenReturn(List.of(workerResponse("w1"), workerResponse("w2"), workerResponse("w3")));
        // Only CONFIRMED / IN_PROGRESS tours are loaded: drafts (PLANNED) and cancelled tours never reserve
        when(tourRepository.findByStatusIn(CollectionTourService.RESOURCE_LOCKED_STATUSES)).thenReturn(List.of(
                tour("A", CollectionTourStatus.CONFIRMED, "2026-11-25T09:00:00Z", "2026-11-25T11:00:00Z", List.of("w1"), List.of()),
                tour("B", CollectionTourStatus.CONFIRMED, "2026-11-25T12:00:00Z", "2026-11-25T15:00:00Z", List.of("w2"), List.of())));

        List<String> available = service.getAvailableWorkers(START, END, null).stream().map(WorkerResponse::getId).toList();

        assertEquals(List.of("w2", "w3"), available); // B starts exactly when the period ends: no overlap
        verify(tourRepository, never()).findByStatusIn(argThat(statuses -> statuses.contains(CollectionTourStatus.PLANNED)));
    }

    @Test
    void availableWorkers_ignoreTheTourBeingEdited() {
        when(workerAvailabilityService.getAssignableWorkers(START, END)).thenReturn(List.of(workerResponse("w1")));
        when(tourRepository.findByStatusIn(CollectionTourService.RESOURCE_LOCKED_STATUSES)).thenReturn(List.of(
                tour("A", CollectionTourStatus.CONFIRMED, "2026-11-25T09:00:00Z", "2026-11-25T11:00:00Z", List.of("w1"), List.of())));

        assertEquals(1, service.getAvailableWorkers(START, END, "A").size());
    }

    @Test
    void availableEquipment_excludeEquipmentOfOverlappingConfirmedTours() {
        EquipmentResponse e1 = EquipmentResponse.fromEntity(bin("e1"));
        EquipmentResponse e2 = EquipmentResponse.fromEntity(bin("e2"));
        when(equipmentAvailabilityService.getAssignableEquipment(START, END)).thenReturn(List.of(e1, e2));
        when(tourRepository.findByStatusIn(CollectionTourService.RESOURCE_LOCKED_STATUSES)).thenReturn(List.of(
                tour("A", CollectionTourStatus.IN_PROGRESS, "2026-11-25T08:00:00Z", "2026-11-25T10:00:00Z", List.of(), List.of("e1"))));

        assertEquals(List.of("e2"), service.getAvailableEquipment(START, END, null).stream().map(EquipmentResponse::getId).toList());
    }

    @Test
    void availability_invalidPeriod_isRejectedBeforeLoadingTours() {
        when(workerAvailabilityService.getAssignableWorkers(END, START))
                .thenThrow(new RuntimeException("Start time must be before end time."));

        assertThrows(RuntimeException.class, () -> service.getAvailableWorkers(END, START, null));
        verifyNoInteractions(tourRepository);
    }

    // --- assigning resources to a draft tour ---

    private CollectionTour draftWithTeamLeader() {
        CollectionTour draft = tour("T", CollectionTourStatus.PLANNED, "2026-11-25T07:00:00Z", null, List.of(), List.of());
        when(tourRepository.findById("T")).thenReturn(Optional.of(draft));
        User teamLeader = new User();
        teamLeader.setId("tl");
        teamLeader.setRole(Role.TEAM_LEADER);
        teamLeader.setStatus(AccountStatus.ACTIVE);
        // lenient: a rejected assignment fails before the team leader is checked
        lenient().when(userRepository.findById("tl")).thenReturn(Optional.of(teamLeader));
        return draft;
    }

    private static AssignTourResourcesRequest assign(List<String> workerIds, List<String> equipmentIds) {
        AssignTourResourcesRequest request = new AssignTourResourcesRequest();
        request.setWorkerIds(workerIds);
        request.setEquipmentIds(equipmentIds);
        return request;
    }

    @Test
    void assignResources_doesNotRequireOneItemOfEveryInventoryType_andReturnsResourceDetails() {
        draftWithTeamLeader();
        workersExist();
        binsExist();
        when(workerAvailabilityService.getAssignableWorkers(any(), any()))
                .thenReturn(List.of(workerResponse("w1"), workerResponse("w2")));
        when(equipmentAvailabilityService.getAssignableEquipment(any(), any()))
                .thenReturn(List.of(EquipmentResponse.fromEntity(bin("e1"))));
        when(workerRepository.findAllById(List.of("w1", "w2"))).thenReturn(List.of(worker("w1"), worker("w2")));
        when(equipmentRepository.findAllById(List.of("e1"))).thenReturn(List.of(bin("e1")));

        // The inventory may contain nets, vibrators or custom types: a tour with only a bin is still valid
        CollectionTourResponse response = service.assignResources("T", assign(List.of("w1", "w2"), List.of("e1")));

        assertEquals(List.of("w1", "w2"), response.getAssignedWorkerIds());
        assertEquals(List.of("Worker w1", "Worker w2"),
                response.getAssignedWorkers().stream().map(WorkerResponse::getFullName).toList());
        assertEquals("BN-e1", response.getAssignedEquipment().get(0).getReference());
        verify(equipmentService, never()).getEquipment(any(), anyBoolean());
    }

    @Test
    void assignResources_workerOfAnOverlappingConfirmedTour_isRejected() {
        draftWithTeamLeader();
        workersExist();
        when(workerAvailabilityService.getAssignableWorkers(any(), any()))
                .thenReturn(List.of(workerResponse("w1"), workerResponse("w2")));
        when(equipmentAvailabilityService.getAssignableEquipment(any(), any())).thenReturn(List.of());
        when(tourRepository.findByStatusIn(CollectionTourService.RESOURCE_LOCKED_STATUSES)).thenReturn(List.of(
                tour("A", CollectionTourStatus.CONFIRMED, "2026-11-25T09:00:00Z", "2026-11-25T11:00:00Z", List.of("w2"), List.of())));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.assignResources("T", assign(List.of("w1", "w2"), List.of())));
        assertEquals("Worker w2 is already assigned to an overlapping tour.", ex.getMessage());
        verify(tourRepository, never()).save(any());
    }

    // --- equipment IN_USE (#25): in use right now, during a running confirmed tour ---

    @Test
    void equipmentStatus_availableEquipmentInARunningConfirmedTour_isInUse() {
        Instant now = Instant.now();
        when(equipmentAvailabilityService.getCurrentStatus("e1"))
                .thenReturn(new EquipmentStatusResponse("e1", EquipmentStatus.AVAILABLE, true));
        // Only CONFIRMED / IN_PROGRESS tours are loaded: drafts, cancelled and completed tours never make equipment in use
        when(tourRepository.findByStatusIn(CollectionTourService.RESOURCE_LOCKED_STATUSES)).thenReturn(List.of(
                tour("A", CollectionTourStatus.CONFIRMED, now.minusSeconds(3600).toString(), now.plusSeconds(3600).toString(), List.of(), List.of("e1"))));

        assertEquals(EquipmentStatus.IN_USE, service.getEquipmentStatus("e1").getStatus());
    }

    @Test
    void equipmentStatus_withoutRunningTour_staysAvailable() {
        Instant now = Instant.now();
        when(equipmentAvailabilityService.getCurrentStatus("e1"))
                .thenReturn(new EquipmentStatusResponse("e1", EquipmentStatus.AVAILABLE, true));
        when(tourRepository.findByStatusIn(CollectionTourService.RESOURCE_LOCKED_STATUSES)).thenReturn(List.of(
                tour("Later", CollectionTourStatus.CONFIRMED, now.plusSeconds(3600).toString(), now.plusSeconds(7200).toString(), List.of(), List.of("e1")),
                tour("Other", CollectionTourStatus.CONFIRMED, now.minusSeconds(3600).toString(), now.plusSeconds(3600).toString(), List.of(), List.of("e2"))));

        assertEquals(EquipmentStatus.AVAILABLE, service.getEquipmentStatus("e1").getStatus());
    }

    @Test
    void equipmentStatus_maintenanceOrUnavailable_winsOverInUse() {
        when(equipmentAvailabilityService.getCurrentStatus("e1"))
                .thenReturn(new EquipmentStatusResponse("e1", EquipmentStatus.UNDER_MAINTENANCE, true));
        when(equipmentAvailabilityService.getCurrentStatus("e2"))
                .thenReturn(new EquipmentStatusResponse("e2", EquipmentStatus.UNAVAILABLE, false));

        assertEquals(EquipmentStatus.UNDER_MAINTENANCE, service.getEquipmentStatus("e1").getStatus());
        assertEquals(EquipmentStatus.UNAVAILABLE, service.getEquipmentStatus("e2").getStatus());
        verify(tourRepository, never()).findByStatusIn(any());
    }

    @Test
    void runningTour_windowIncludesItsStartButNotItsEnd() {
        when(tourRepository.findByStatusIn(CollectionTourService.RESOURCE_LOCKED_STATUSES)).thenReturn(List.of(
                tour("A", CollectionTourStatus.CONFIRMED, "2026-11-25T07:00:00Z", "2026-11-25T11:30:00Z", List.of(), List.of("e1"))));

        assertFalse(service.isInRunningTour("e1", Instant.parse("2026-11-25T06:59:59Z")));
        assertTrue(service.isInRunningTour("e1", Instant.parse("2026-11-25T07:00:00Z")));
        assertTrue(service.isInRunningTour("e1", Instant.parse("2026-11-25T11:29:59Z")));
        assertFalse(service.isInRunningTour("e1", Instant.parse("2026-11-25T11:30:00Z")));
        assertFalse(service.isInRunningTour("e2", Instant.parse("2026-11-25T09:00:00Z")));
    }

    // confirmTour runs validateTourResourcesAvailable: drafts don't block each other, the first confirmed wins
    @Test
    void twoDraftsSharingAWorker_firstConfirmedWins() {
        CollectionTour draftB = tour("B", CollectionTourStatus.PLANNED, "2026-11-25T08:00:00Z", "2026-11-25T12:00:00Z", List.of("w1"), List.of());
        CollectionTour confirmedA = tour("A", CollectionTourStatus.CONFIRMED, "2026-11-25T09:00:00Z", "2026-11-25T11:00:00Z", List.of("w1"), List.of());
        workersExist();
        when(workerAvailabilityService.getAssignableWorkers(any(), any())).thenReturn(List.of(workerResponse("w1")));
        when(equipmentAvailabilityService.getAssignableEquipment(any(), any())).thenReturn(List.of());
        when(tourRepository.findByStatusIn(CollectionTourService.RESOURCE_LOCKED_STATUSES))
                .thenReturn(List.of())                // A is still a draft: nothing is reserved
                .thenReturn(List.of(confirmedA));     // A has been confirmed

        assertDoesNotThrow(() -> service.validateTourResourcesAvailable(draftB));
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.validateTourResourcesAvailable(draftB));
        assertEquals("Worker w1 is already assigned to an overlapping tour.", ex.getMessage());
    }

    // --- team leaders: one confirmed tour at a time ---

    private User leader(String id, String name, Role role, AccountStatus status) {
        User user = new User();
        user.setId(id);
        user.setFullName(name);
        user.setRole(role);
        user.setStatus(status);
        lenient().when(userRepository.findById(id)).thenReturn(Optional.of(user));
        return user;
    }

    private User activeLeader(String id, String name) {
        return leader(id, name, Role.TEAM_LEADER, AccountStatus.ACTIVE);
    }

    private static CollectionTour ledBy(String leaderId, CollectionTour tour) {
        tour.setTeamLeaderId(leaderId);
        return tour;
    }

    private void confirmedTours(CollectionTour... tours) {
        when(tourRepository.findByStatusIn(CollectionTourService.RESOURCE_LOCKED_STATUSES)).thenReturn(List.of(tours));
    }

    private static CollectionTour confirmed(String id, String leaderId, String start, String end) {
        return ledBy(leaderId, tour(id, CollectionTourStatus.CONFIRMED, start, end, List.of(), List.of()));
    }

    private static UpdateTourTeamLeaderRequest changeTo(String leaderId) {
        UpdateTourTeamLeaderRequest request = new UpdateTourTeamLeaderRequest();
        request.setTeamLeaderId(leaderId);
        return request;
    }

    @Test
    void availableTeamLeaders_areActiveFreeAndSortedByName() {
        List<User> leaders = List.of(activeLeader("z", "zied"), activeLeader("busy", "Sami"),
                activeLeader("touching", "Leila"), activeLeader("a", "Amel"));
        when(userRepository.findByRoleAndStatus(Role.TEAM_LEADER, AccountStatus.ACTIVE)).thenReturn(leaders);
        // Only CONFIRMED / IN_PROGRESS tours are loaded: drafts and cancelled tours never block a leader
        confirmedTours(confirmed("A", "busy", "2026-11-25T11:00:00Z", "2026-11-25T14:00:00Z"),
                confirmed("B", "touching", "2026-11-25T12:00:00Z", "2026-11-25T15:00:00Z"));

        assertEquals(List.of("Amel", "Leila", "zied"), service.getAvailableTeamLeaders(START, END).stream()
                .map(TeamLeaderResponse::getFullName).toList());
        verify(tourRepository, never()).findByStatusIn(argThat(statuses -> statuses.contains(CollectionTourStatus.PLANNED)));
    }

    @Test
    void availableTeamLeaders_invalidPeriod_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.getAvailableTeamLeaders(END, START));
        assertThrows(IllegalArgumentException.class, () -> service.getAvailableTeamLeaders(START, START));
        verifyNoInteractions(tourRepository, userRepository);
    }

    private CreateCollectionTourRequest newTourLedBySami() {
        activeLeader("sami", "Sami");
        workersExist();
        when(workerAvailabilityService.getAssignableWorkers(any(), any()))
                .thenReturn(List.of(workerResponse("w1"), workerResponse("w2")));
        // The real end is 12:00Z, not the start + 8 h default (15:00Z)
        doAnswer(inv -> {
            inv.<CollectionTour>getArgument(0).setEstimatedEndTime(END);
            return null;
        }).when(scheduleCalculationService).recalculateTour(any());
        CreateCollectionTourRequest request = new CreateCollectionTourRequest();
        request.setTitle("Tour");
        request.setTeamLeaderId("sami");
        request.setPlannedStartTime(START);
        request.setWorkerIds(List.of("w1", "w2"));
        return request;
    }

    @Test
    void createTour_withABusyLeader_isRejected() {
        CreateCollectionTourRequest request = newTourLedBySami();
        confirmedTours(confirmed("A", "sami", "2026-11-25T11:00:00Z", "2026-11-25T14:00:00Z"));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.createTour(request, "cm"));
        assertEquals("Team leader 'Sami' is already assigned to an overlapping tour.", ex.getMessage());
        verify(tourRepository, never()).save(any());
    }

    @Test
    void createTour_checksTheLeaderAgainstTheCalculatedEnd() {
        CreateCollectionTourRequest request = newTourLedBySami();
        confirmedTours(confirmed("A", "sami", "2026-11-25T12:00:00Z", "2026-11-25T14:00:00Z"));

        assertEquals("sami", service.createTour(request, "cm").getTeamLeaderId());
    }

    @Test
    void updateTeamLeader_toAFreeLeader_isSaved() {
        ledBy("sami", draftWithTeamLeader());
        activeLeader("leila", "Leila");
        confirmedTours(confirmed("A", "sami", "2026-11-25T09:00:00Z", "2026-11-25T11:00:00Z"));

        assertEquals("leila", service.updateTeamLeader("T", changeTo("leila")).getTeamLeaderId());
        verify(tourRepository).save(argThat(saved -> "leila".equals(saved.getTeamLeaderId())));
    }

    @Test
    void updateTeamLeader_refusesABusyInactiveUnknownOrNonLeaderUser() {
        draftWithTeamLeader();
        activeLeader("busy", "Sami");
        leader("inactive", "Leila", Role.TEAM_LEADER, AccountStatus.PENDING_APPROVAL);
        leader("farmer", "Amel", Role.FARMER, AccountStatus.ACTIVE);
        confirmedTours(confirmed("A", "busy", "2026-11-25T09:00:00Z", "2026-11-25T11:00:00Z"));

        assertThrows(IllegalStateException.class, () -> service.updateTeamLeader("T", changeTo("busy")));
        assertThrows(IllegalArgumentException.class, () -> service.updateTeamLeader("T", changeTo("inactive")));
        assertThrows(IllegalArgumentException.class, () -> service.updateTeamLeader("T", changeTo("farmer")));
        assertThrows(ResourceNotFoundException.class, () -> service.updateTeamLeader("T", changeTo("unknown")));
        verify(tourRepository, never()).save(any());
    }

    @Test
    void updateTeamLeader_onlyOnADraft() {
        when(tourRepository.findById("A")).thenReturn(Optional.of(
                confirmed("A", "sami", "2026-11-25T09:00:00Z", "2026-11-25T11:00:00Z")));

        assertThrows(IllegalStateException.class, () -> service.updateTeamLeader("A", changeTo("leila")));
        verify(tourRepository, never()).save(any());
    }

    private CollectionTour confirmableDraft(String id, String worker1, String worker2, String equipmentId) {
        CollectionTour draft = ledBy("sami", tour(id, CollectionTourStatus.PLANNED, START.toString(), END.toString(),
                List.of(worker1, worker2), List.of(equipmentId)));
        TourStop stop = new TourStop();
        stop.setHarvestRequestId("hr-" + id);
        stop.setOrchardId("o-" + id);
        stop.setEstimatedQuantityKg(400);
        OilMillDropOff dropOff = new OilMillDropOff();
        dropOff.setOilMillId("mill");
        dropOff.setAllocatedQuantityKg(400);
        draft.setStops(List.of(stop));
        draft.setDropOffs(List.of(dropOff));
        lenient().when(tourRepository.findById(id)).thenReturn(Optional.of(draft));
        return draft;
    }

    // Different workers and bins: only the leader conflicts
    @Test
    void twoDraftsSharingALeader_firstConfirmedWins_andTheSecondIsRepairedByChangingItsLeader() {
        activeLeader("sami", "Sami");
        activeLeader("leila", "Leila");
        CollectionTour a = confirmableDraft("A", "w1", "w2", "e1");
        CollectionTour b = confirmableDraft("B", "w3", "w4", "e2");
        database(List.of(a, b), request("hr-A", "o-A"), request("hr-B", "o-B"));

        service.confirmTour("A");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.confirmTour("B"));
        assertEquals("Team leader 'Sami' is already assigned to an overlapping tour.", ex.getMessage());
        assertEquals(CollectionTourStatus.PLANNED, b.getStatus());
        verify(tourRepository, never()).save(b);

        service.updateTeamLeader("B", changeTo("leila"));
        assertEquals(CollectionTourStatus.CONFIRMED, service.confirmTour("B").getStatus());
    }

    @Test
    void tourResponse_carriesTheLeaderName_evenWhenMissing() {
        User sami = activeLeader("sami", "Sami");
        when(userRepository.findAllById(List.of("sami", "deleted"))).thenReturn(List.of(sami));
        when(tourRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(
                confirmed("A", "sami", "2026-11-25T09:00:00Z", "2026-11-25T11:00:00Z"),
                confirmed("B", "deleted", "2026-11-25T09:00:00Z", "2026-11-25T11:00:00Z"),
                confirmed("C", null, "2026-11-25T09:00:00Z", "2026-11-25T11:00:00Z")));

        List<CollectionTourResponse> tours = service.getTours(null);

        assertEquals("Sami", tours.get(0).getTeamLeaderName());
        assertNull(tours.get(1).getTeamLeaderName());
        assertNull(tours.get(2).getTeamLeaderName());
    }

    @Test
    void getTours_loadsLeadersWorkersAndEquipmentOnceForAllTours() {
        User sami = activeLeader("sami", "Sami");
        List<CollectionTour> tours = java.util.stream.IntStream.range(0, 6)
                .mapToObj(i -> ledBy("sami", tour("T" + i, CollectionTourStatus.PLANNED, "2026-11-25T07:00:00Z", null,
                        List.of("w2", "w1", "gone"), List.of("e1")))).toList();
        tours.get(5).setAssignedWorkerIds(null);
        when(tourRepository.findAllByOrderByCreatedAtDesc()).thenReturn(tours);
        when(userRepository.findAllById(any())).thenReturn(List.of(sami));
        when(workerRepository.findAllById(any())).thenReturn(List.of(worker("w1"), worker("w2")));
        when(equipmentRepository.findAllById(any())).thenReturn(List.of(bin("e1")));

        List<CollectionTourResponse> responses = service.getTours(null);

        assertEquals(6, responses.size());
        assertEquals("Sami", responses.get(0).getTeamLeaderName());
        assertEquals(List.of("w2", "w1"), responses.get(0).getAssignedWorkers().stream().map(WorkerResponse::getId).toList());
        assertEquals("BN-e1", responses.get(4).getAssignedEquipment().get(0).getReference());
        assertTrue(responses.get(5).getAssignedWorkers().isEmpty());
        verify(userRepository, times(1)).findAllById(any());
        verify(userRepository, never()).findById(any());
        verify(workerRepository, times(1)).findAllById(List.of("w2", "w1", "gone"));
        verify(equipmentRepository, times(1)).findAllById(List.of("e1"));
    }

    @Test
    void getTours_withoutTours_asksNothingElse() {
        when(tourRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());
        assertTrue(service.getTours(null).isEmpty());
        verifyNoInteractions(userRepository, workerRepository, equipmentRepository);
    }

    // Lists are read without their route: saving one would erase it
    @Test
    void cancelTour_savesOnlyTheCancelledTour() {
        CollectionTour a = draftA();
        CollectionTour b = draftBOnOrchardOfA("hr-B");
        a.setStatus(CollectionTourStatus.CONFIRMED);
        b.setStatus(CollectionTourStatus.CONFIRMED);
        database(List.of(a, b), request("hr-A", "o-A"), request("hr-B", "o-A"));

        service.cancelTour("A", null);

        verify(tourRepository).save(a);
        verify(tourRepository, never()).save(b);
        verify(tourRepository, never()).saveAll(any());
    }

    // --- confirmation: one open tour per orchard, requests still approved ---

    private static HarvestRequest request(String id, String orchardId) {
        HarvestRequest request = new HarvestRequest();
        request.setId(id);
        request.setOrchardId(orchardId);
        request.setStatus(HarvestRequestStatus.APPROVED);
        return request;
    }

    private static <T> List<T> pick(Iterable<String> ids, Map<String, T> byId) {
        return StreamSupport.stream(ids.spliterator(), false).filter(byId::containsKey).map(byId::get).toList();
    }

    // Answers like the real repositories. Orchard ids are "o-A" / "o-B", named "Verger o-A" / "Verger o-B"
    private Map<String, Orchard> database(List<CollectionTour> tours, HarvestRequest... requests) {
        OilMill mill = new OilMill();
        mill.setId("mill");
        mill.setName("Mill");
        mill.setActive(true);
        mill.setDailyCapacityKg(5000);
        lenient().when(oilMillService.findOilMills(any())).thenReturn(Map.of("mill", mill));
        workersExist();
        binsExist();
        lenient().when(workerAvailabilityService.getAssignableWorkers(any(), any())).thenReturn(List.of(
                workerResponse("w1"), workerResponse("w2"), workerResponse("w3"), workerResponse("w4")));
        lenient().when(equipmentAvailabilityService.getAssignableEquipment(any(), any())).thenReturn(List.of(
                EquipmentResponse.fromEntity(bin("e1")), EquipmentResponse.fromEntity(bin("e2"))));
        lenient().when(tourRepository.findByStatusIn(any())).thenAnswer(inv -> tours.stream()
                .filter(t -> inv.<List<CollectionTourStatus>>getArgument(0).contains(t.getStatus())).toList());

        Map<String, HarvestRequest> requestsById = new HashMap<>();
        for (HarvestRequest request : requests) {
            requestsById.put(request.getId(), request);
        }
        lenient().when(harvestRequestRepository.findAllById(any()))
                .thenAnswer(inv -> pick(inv.getArgument(0), requestsById));

        Map<String, Orchard> orchardsById = new HashMap<>();
        for (String id : List.of("o-A", "o-B")) {
            Orchard orchard = new Orchard();
            orchard.setId(id);
            orchard.setName("Verger " + id);
            orchardsById.put(id, orchard);
        }
        lenient().when(orchardRepository.findAllById(any())).thenAnswer(inv -> pick(inv.getArgument(0), orchardsById));
        lenient().when(orchardRepository.findById(any()))
                .thenAnswer(inv -> Optional.ofNullable(orchardsById.get(inv.<String>getArgument(0))));
        return orchardsById;
    }

    // A and B differ in leader, workers, bin and day: only what the test makes them share can conflict
    private CollectionTour draftA() {
        activeLeader("sami", "Sami");
        CollectionTour a = confirmableDraft("A", "w1", "w2", "e1");
        a.setTitle("Tournée A");
        return a;
    }

    private CollectionTour draftBOnOrchardOfA(String requestId) {
        activeLeader("leila", "Leila");
        CollectionTour b = ledBy("leila", confirmableDraft("B", "w3", "w4", "e2"));
        b.setPlannedStartTime(START.plusSeconds(86400));
        b.setEstimatedEndTime(END.plusSeconds(86400));
        b.getStops().get(0).setOrchardId("o-A");
        b.getStops().get(0).setHarvestRequestId(requestId);
        return b;
    }

    private void assertConfirmRefused(CollectionTour draft, String message) {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.confirmTour(draft.getId()));
        assertEquals(message, ex.getMessage());
        assertEquals(CollectionTourStatus.PLANNED, draft.getStatus());
        verify(tourRepository, never()).save(draft);
    }

    @Test
    void confirm_schedulesTheRequestAndItsOrchard() {
        CollectionTour a = draftA();
        HarvestRequest request = request("hr-A", "o-A");
        Map<String, Orchard> orchards = database(List.of(a), request);

        assertEquals(CollectionTourStatus.CONFIRMED, service.confirmTour("A").getStatus());
        assertEquals(HarvestRequestStatus.SCHEDULED, request.getStatus());
        assertEquals(OrchardHarvestStatus.SCHEDULED, orchards.get("o-A").getHarvestStatus());
    }

    @Test
    void confirm_readsEachKindOfDataOnce_andCalculatesTheRouteOnce() {
        CollectionTour a = draftA();
        database(List.of(a), request("hr-A", "o-A"));

        service.confirmTour("A");

        verify(scheduleCalculationService, times(1)).recalculateTour(a);
        // Workers: one read to check them, one for the reply; never one per worker
        verify(workerRepository, times(2)).findAllById(any());
        verify(workerService, never()).findWorker(any());
        verify(equipmentService, never()).findEquipment(any());
        // Confirmed tours once for every check, and once more for the mills' capacity
        verify(tourRepository, times(2)).findByStatusIn(any());
        verify(oilMillService, times(1)).findOilMills(any());
    }

    @Test
    void confirm_unknownWorker_isRefusedAndNothingIsSaved() {
        CollectionTour a = draftA();
        a.setAssignedWorkerIds(List.of("w1", "gone"));
        database(List.of(a), request("hr-A", "o-A"));
        lenient().doReturn(List.of(worker("w1"))).when(workerRepository).findAllById(any());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.confirmTour("A"));

        assertEquals("Worker not found with id: gone", ex.getMessage());
        assertEquals(CollectionTourStatus.PLANNED, a.getStatus());
        verify(tourRepository, never()).save(a);
    }

    @Test
    void confirm_inactiveMill_isStillRefused() {
        CollectionTour a = draftA();
        database(List.of(a), request("hr-A", "o-A"));
        OilMill closed = new OilMill();
        closed.setId("mill");
        closed.setName("Mill");
        closed.setActive(false);
        closed.setDailyCapacityKg(5000);
        lenient().when(oilMillService.findOilMills(any())).thenReturn(Map.of("mill", closed));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.confirmTour("A"));

        assertEquals("Oil mill is inactive: Mill", ex.getMessage());
        verify(tourRepository, never()).save(a);
    }

    @Test
    void twoDraftsSharingARequest_secondIsRefused_untilTheFirstIsCancelled() {
        CollectionTour a = draftA();
        CollectionTour b = draftBOnOrchardOfA("hr-A");
        database(List.of(a, b), request("hr-A", "o-A"));

        service.confirmTour("A");
        assertConfirmRefused(b, "Cannot confirm: orchard 'Verger o-A' is already in the confirmed tour 'Tournée A'.");

        service.cancelTour("A", null);
        assertEquals(CollectionTourStatus.CONFIRMED, service.confirmTour("B").getStatus());
    }

    @Test
    void twoDraftsSharingAnOrchardThroughDifferentRequests_secondIsRefused() {
        CollectionTour a = draftA();
        CollectionTour b = draftBOnOrchardOfA("hr-B");
        database(List.of(a, b), request("hr-A", "o-A"), request("hr-B", "o-A"));

        service.confirmTour("A");
        assertConfirmRefused(b, "Cannot confirm: orchard 'Verger o-A' is already in the confirmed tour 'Tournée A'.");
    }

    @Test
    void orchardOfAnInProgressTour_isRefused_butDraftCancelledAndCompletedToursNeverBlock() {
        CollectionTour a = draftA();
        CollectionTour b = draftBOnOrchardOfA("hr-B");
        HarvestRequest requestB = request("hr-B", "o-A");
        database(List.of(a, b), request("hr-A", "o-A"), requestB);

        a.setStatus(CollectionTourStatus.IN_PROGRESS);
        assertConfirmRefused(b, "Cannot confirm: orchard 'Verger o-A' is already in the confirmed tour 'Tournée A'.");

        for (CollectionTourStatus status : List.of(CollectionTourStatus.PLANNED,
                CollectionTourStatus.CANCELLED, CollectionTourStatus.COMPLETED)) {
            a.setStatus(status);
            b.setStatus(CollectionTourStatus.PLANNED);
            requestB.setStatus(HarvestRequestStatus.APPROVED);
            assertEquals(CollectionTourStatus.CONFIRMED, service.confirmTour("B").getStatus(), "other tour " + status);
        }
    }

    @Test
    void confirm_requestNoLongerApproved_isRefused() {
        CollectionTour a = draftA();
        HarvestRequest request = request("hr-A", "o-A");
        database(List.of(a), request);

        for (HarvestRequestStatus status : HarvestRequestStatus.values()) {
            if (status == HarvestRequestStatus.APPROVED) {
                continue;
            }
            request.setStatus(status);
            assertConfirmRefused(a, "Cannot confirm: the harvest request for orchard 'Verger o-A' is " + status + ", not APPROVED.");
            assertEquals(status, request.getStatus());
        }
    }

    @Test
    void confirm_missingRequest_isRefused() {
        CollectionTour a = draftA();
        database(List.of(a));

        assertConfirmRefused(a, "Cannot confirm: harvest request hr-A no longer exists.");
        verifyNoInteractions(scheduleCalculationService);
    }

    // --- stops and drop-offs of a draft may differ until confirmation ---

    private static UpdateTourStopsRequest stops(String... requestIds) {
        UpdateTourStopsRequest request = new UpdateTourStopsRequest();
        request.setOrderedHarvestRequestIds(List.of(requestIds));
        return request;
    }

    private static CreateCollectionTourRequest.OilMillDropOffInput toMill(double quantityKg) {
        CreateCollectionTourRequest.OilMillDropOffInput input = new CreateCollectionTourRequest.OilMillDropOffInput();
        input.setOilMillId("mill");
        input.setAllocatedQuantityKg(quantityKg);
        return input;
    }

    private static UpdateTourDropOffsRequest dropOffs(double quantityKg) {
        UpdateTourDropOffsRequest request = new UpdateTourDropOffsRequest();
        request.setDropOffs(List.of(toMill(quantityKg)));
        return request;
    }

    private static HarvestRequest request(String id, String orchardId, double quantityKg) {
        HarvestRequest request = request(id, orchardId);
        request.setEstimatedQuantityKg(quantityKg);
        return request;
    }

    // Stops: 200 kg (o-A) + 300 kg (o-B). Drop-offs: 500 kg
    private CollectionTour draftWithTwoStops() {
        CollectionTour draft = draftA();
        database(List.of(draft), request("hr-A", "o-A", 200), request("hr-B", "o-B", 300));
        draft.getDropOffs().get(0).setAllocatedQuantityKg(500);
        draft.getStops().get(0).setEstimatedQuantityKg(500);
        service.updateStops("A", stops("hr-A", "hr-B"));
        return draft;
    }

    @Test
    void removeAStopThenReduceTheMills_draftCanBeConfirmed_onlyOnceEqual() {
        CollectionTour draft = draftWithTwoStops();

        service.updateStops("A", stops("hr-B"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.confirmTour("A"));
        assertEquals("Allocated oil mill drop-off quantity (500.0 kg) must equal the estimated harvest quantity (300.0 kg).",
                ex.getMessage());
        assertEquals(CollectionTourStatus.PLANNED, draft.getStatus());

        service.updateDropOffs("A", dropOffs(300));
        assertEquals(CollectionTourStatus.CONFIRMED, service.confirmTour("A").getStatus());
    }

    @Test
    void reduceTheMillsThenRemoveAStop_draftCanBeConfirmed() {
        draftWithTwoStops();

        service.updateDropOffs("A", dropOffs(300));
        service.updateStops("A", stops("hr-B"));
        assertEquals(CollectionTourStatus.CONFIRMED, service.confirmTour("A").getStatus());
    }

    @Test
    void draftEdit_stillChecksBinCapacity() {
        CollectionTour draft = draftA();
        database(List.of(draft), request("hr-A", "o-A", 501));

        assertThrows(IllegalArgumentException.class, () -> service.updateStops("A", stops("hr-A")));
        verify(tourRepository, never()).save(any());
    }

    @Test
    void createTour_withUnequalQuantities_isRejected() {
        CreateCollectionTourRequest request = newTourLedBySami();
        database(List.of(), request("hr-A", "o-A", 400));
        request.setHarvestRequestIds(List.of("hr-A"));
        request.setEquipmentIds(List.of("e1"));
        request.setDropOffs(List.of(toMill(300)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.createTour(request, "cm"));
        assertTrue(ex.getMessage().contains("must equal"), ex.getMessage());
        verify(tourRepository, never()).save(any());
    }

    // --- a draft is deleted, a confirmed tour is cancelled ---

    @Test
    void deleteDraft_removesItAndChangesNothingElse() {
        CollectionTour draft = draftA();

        service.deleteDraft("A");

        verify(tourRepository).delete(draft);
        verify(tourRepository, never()).save(any());
        verifyNoInteractions(harvestRequestRepository, orchardRepository);
    }

    @Test
    void deleteDraft_refusesEveryOtherStatusAndAnUnknownTour() {
        CollectionTour tour = draftA();
        for (CollectionTourStatus status : CollectionTourStatus.values()) {
            if (status == CollectionTourStatus.PLANNED) {
                continue;
            }
            tour.setStatus(status);
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.deleteDraft("A"));
            assertEquals("Only PLANNED tours can be deleted. Current status: " + status, ex.getMessage());
        }
        assertThrows(ResourceNotFoundException.class, () -> service.deleteDraft("unknown"));
        verify(tourRepository, never()).delete(any());
    }

    @Test
    void cancelTour_refusesADraft() {
        CollectionTour draft = draftA();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.cancelTour("A", null));
        assertEquals("Only confirmed tours can be cancelled. Delete the draft instead.", ex.getMessage());
        assertEquals(CollectionTourStatus.PLANNED, draft.getStatus());
        verify(tourRepository, never()).save(any());
    }

    @Test
    void cancelTour_confirmedOrInProgress_isKeptAsCancelled_andItsRequestIsApprovedAgain() {
        CollectionTour tour = draftA();
        HarvestRequest request = request("hr-A", "o-A");
        database(List.of(tour), request);

        for (CollectionTourStatus status : CollectionTourService.RESOURCE_LOCKED_STATUSES) {
            tour.setStatus(status);
            request.setStatus(HarvestRequestStatus.SCHEDULED);
            assertEquals(CollectionTourStatus.CANCELLED, service.cancelTour("A", null).getStatus());
            assertEquals(HarvestRequestStatus.APPROVED, request.getStatus());
        }
        verify(tourRepository, never()).delete(any());
    }
}
