package ISIMS.olive_connect.service;

import ISIMS.olive_connect.dto.AssignTourResourcesRequest;
import ISIMS.olive_connect.dto.CollectionTourResponse;
import ISIMS.olive_connect.dto.EquipmentResponse;
import ISIMS.olive_connect.dto.WorkerResponse;
import ISIMS.olive_connect.entity.AccountStatus;
import ISIMS.olive_connect.entity.CollectionTour;
import ISIMS.olive_connect.entity.CollectionTourStatus;
import ISIMS.olive_connect.entity.Equipment;
import ISIMS.olive_connect.entity.Role;
import ISIMS.olive_connect.entity.User;
import ISIMS.olive_connect.entity.Worker;
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
import java.util.List;
import java.util.Optional;

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
        when(workerService.findWorker(any())).thenAnswer(inv -> worker(inv.getArgument(0)));
        when(equipmentService.findEquipment("e1")).thenReturn(bin("e1"));
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
        when(workerService.findWorker(any())).thenAnswer(inv -> worker(inv.getArgument(0)));
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

    // confirmTour runs validateTourResourcesAvailable: drafts don't block each other, the first confirmed wins
    @Test
    void twoDraftsSharingAWorker_firstConfirmedWins() {
        CollectionTour draftB = tour("B", CollectionTourStatus.PLANNED, "2026-11-25T08:00:00Z", "2026-11-25T12:00:00Z", List.of("w1"), List.of());
        CollectionTour confirmedA = tour("A", CollectionTourStatus.CONFIRMED, "2026-11-25T09:00:00Z", "2026-11-25T11:00:00Z", List.of("w1"), List.of());
        when(workerService.findWorker("w1")).thenReturn(worker("w1"));
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
}
