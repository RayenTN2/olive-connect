package ISIMS.olive_connect.service;

import ISIMS.olive_connect.dto.ResourceCalendarResponse;
import ISIMS.olive_connect.dto.ResourceCalendarResponse.Entry;
import ISIMS.olive_connect.dto.ResourceCalendarResponse.EntryKind;
import ISIMS.olive_connect.entity.CollectionTour;
import ISIMS.olive_connect.entity.CollectionTourStatus;
import ISIMS.olive_connect.entity.Equipment;
import ISIMS.olive_connect.entity.EquipmentUnavailability;
import ISIMS.olive_connect.entity.EquipmentUnavailabilityStatus;
import ISIMS.olive_connect.entity.Worker;
import ISIMS.olive_connect.entity.WorkerUnavailability;
import ISIMS.olive_connect.repository.CollectionTourRepository;
import ISIMS.olive_connect.repository.EquipmentRepository;
import ISIMS.olive_connect.repository.EquipmentUnavailabilityRepository;
import ISIMS.olive_connect.repository.WorkerRepository;
import ISIMS.olive_connect.repository.WorkerUnavailabilityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceCalendarServiceTest {

    private static final Instant START = Instant.parse("2026-11-25T00:00:00Z");
    private static final Instant END = Instant.parse("2026-11-26T00:00:00Z");

    @Mock private WorkerRepository workerRepository;
    @Mock private EquipmentRepository equipmentRepository;
    @Mock private WorkerUnavailabilityRepository workerUnavailabilityRepository;
    @Mock private EquipmentUnavailabilityRepository equipmentUnavailabilityRepository;
    @Mock private CollectionTourRepository tourRepository;

    @InjectMocks
    private ResourceCalendarService service;

    @BeforeEach
    void setUp() {
        lenient().when(workerRepository.findAll()).thenReturn(List.of(worker("w1", true)));
        lenient().when(equipmentRepository.findAll()).thenReturn(List.of());
    }

    private static Worker worker(String id, boolean active) {
        Worker worker = new Worker();
        worker.setId(id);
        worker.setFullName("Worker " + id);
        worker.setActive(active);
        return worker;
    }

    private static CollectionTour tour(String id, String start, String end, List<String> workerIds, List<String> equipmentIds) {
        CollectionTour tour = new CollectionTour();
        tour.setId(id);
        tour.setTitle("Tour " + id);
        tour.setStatus(CollectionTourStatus.CONFIRMED);
        tour.setPlannedStartTime(Instant.parse(start));
        tour.setEstimatedEndTime(end != null ? Instant.parse(end) : null);
        tour.setAssignedWorkerIds(workerIds);
        tour.setAssignedEquipmentIds(equipmentIds);
        return tour;
    }

    private static WorkerUnavailability absence(String workerId, String start, String end) {
        WorkerUnavailability period = new WorkerUnavailability();
        period.setWorkerId(workerId);
        period.setStartTime(Instant.parse(start));
        period.setEndTime(Instant.parse(end));
        period.setReason("sick");
        return period;
    }

    private void givenTours(CollectionTour... tours) {
        when(tourRepository.findByStatusIn(CollectionTourService.CAPACITY_RESERVED_STATUSES)).thenReturn(List.of(tours));
    }

    // Behaves like the real query: periods with startTime < rangeEnd AND endTime > rangeStart
    private void givenAbsences(WorkerUnavailability... periods) {
        lenient().when(workerUnavailabilityRepository.findByStartTimeLessThanAndEndTimeGreaterThan(any(), any()))
                .thenAnswer(inv -> {
                    Instant rangeEnd = inv.getArgument(0);
                    Instant rangeStart = inv.getArgument(1);
                    return Stream.of(periods)
                            .filter(p -> p.getStartTime().isBefore(rangeEnd) && p.getEndTime().isAfter(rangeStart))
                            .toList();
                });
    }

    private List<Entry> entriesOf(List<ResourceCalendarResponse> calendar, String id) {
        return calendar.stream().filter(r -> r.getId().equals(id)).findFirst().orElseThrow().getEntries();
    }

    @Test
    void tourOverlappingAnAbsence_bothEntriesAreConflicts() {
        givenTours(tour("A", "2026-11-25T07:00:00Z", "2026-11-25T12:00:00Z", List.of("w1"), List.of()));
        givenAbsences(absence("w1", "2026-11-25T10:00:00Z", "2026-11-25T18:00:00Z"));

        List<Entry> entries = entriesOf(service.getCalendar(START, END), "w1");

        assertEquals(List.of(EntryKind.TOUR, EntryKind.UNAVAILABLE), entries.stream().map(Entry::getKind).toList());
        assertTrue(entries.get(0).isConflict());
        assertTrue(entries.get(1).isConflict());
        assertEquals("A", entries.get(0).getTourId());
        assertEquals("Tour A", entries.get(0).getTourTitle());
        assertEquals(CollectionTourStatus.CONFIRMED, entries.get(0).getTourStatus());
        assertEquals("sick", entries.get(1).getReason());
    }

    @Test
    void twoOverlappingToursOnTheSameWorker_areConflicts() {
        givenTours(tour("A", "2026-11-25T07:00:00Z", "2026-11-25T12:00:00Z", List.of("w1"), List.of()),
                tour("B", "2026-11-25T11:00:00Z", "2026-11-25T15:00:00Z", List.of("w1"), List.of()));
        givenAbsences();

        List<Entry> entries = entriesOf(service.getCalendar(START, END), "w1");

        assertEquals(2, entries.size());
        assertTrue(entries.stream().allMatch(Entry::isConflict));
    }

    @Test
    void entriesTouchingAtTheirBoundary_areNotConflicts() {
        givenTours(tour("A", "2026-11-25T07:00:00Z", "2026-11-25T12:00:00Z", List.of("w1"), List.of()),
                tour("B", "2026-11-25T12:00:00Z", "2026-11-25T15:00:00Z", List.of("w1"), List.of()));
        givenAbsences(absence("w1", "2026-11-25T15:00:00Z", "2026-11-25T20:00:00Z"));

        List<Entry> entries = entriesOf(service.getCalendar(START, END), "w1");

        assertEquals(3, entries.size());
        assertTrue(entries.stream().noneMatch(Entry::isConflict));
    }

    @Test
    void entriesCrossingTheRange_keepTheirActualTimes_andToursOutsideOrTouchingTheRangeAreLeftOut() {
        givenTours(tour("A", "2026-11-24T20:00:00Z", "2026-11-25T02:00:00Z", List.of("w1"), List.of()), // starts before
                tour("B", "2026-11-24T10:00:00Z", "2026-11-25T00:00:00Z", List.of("w1"), List.of()),  // ends at range start
                tour("C", "2026-11-26T00:00:00Z", "2026-11-26T05:00:00Z", List.of("w1"), List.of())); // starts at range end
        givenAbsences(absence("w1", "2026-11-25T22:00:00Z", "2026-11-27T00:00:00Z"));               // ends after

        List<Entry> entries = entriesOf(service.getCalendar(START, END), "w1");

        assertEquals(2, entries.size());
        assertEquals(Instant.parse("2026-11-24T20:00:00Z"), entries.get(0).getStart());
        assertEquals(Instant.parse("2026-11-25T02:00:00Z"), entries.get(0).getEnd());
        assertEquals(Instant.parse("2026-11-27T00:00:00Z"), entries.get(1).getEnd());
        // Hidden tours still count: A overlaps B, and the absence overlaps C
        assertTrue(entries.stream().allMatch(Entry::isConflict));
    }

    @Test
    void entryCrossingTheRangeStart_isFlaggedForAClashOutsideTheRange_whichItselfIsNotShown() {
        givenTours(tour("A", "2026-11-24T20:00:00Z", "2026-11-25T02:00:00Z", List.of("w1"), List.of()));
        givenAbsences(absence("w1", "2026-11-24T18:00:00Z", "2026-11-24T21:00:00Z")); // ends before the range

        List<Entry> entries = entriesOf(service.getCalendar(START, END), "w1");

        assertEquals(1, entries.size());
        assertEquals("A", entries.get(0).getTourId());
        assertTrue(entries.get(0).isConflict());
    }

    @Test
    void tourWithoutEstimatedEnd_usesTheSameEightHourWindowAsConflictChecks() {
        givenTours(tour("A", "2026-11-25T07:00:00Z", null, List.of("w1"), List.of()));
        givenAbsences();

        Entry entry = entriesOf(service.getCalendar(START, END), "w1").get(0);

        assertEquals(Instant.parse("2026-11-25T15:00:00Z"), entry.getEnd());
    }

    @Test
    void inactiveResources_areListed_andInactivityIsNotAConflict() {
        Equipment bin = new Equipment();
        bin.setId("e1");
        bin.setName("Bin");
        bin.setReference("BN-1");
        bin.setType("BIN");
        bin.setActive(false);
        when(workerRepository.findAll()).thenReturn(List.of(worker("w1", false), worker("w2", true)));
        when(equipmentRepository.findAll()).thenReturn(List.of(bin));
        givenTours(tour("A", "2026-11-25T07:00:00Z", "2026-11-25T12:00:00Z", List.of("w1"), List.of("e1")));
        givenAbsences();
        EquipmentUnavailability maintenance = new EquipmentUnavailability();
        maintenance.setEquipmentId("e1");
        maintenance.setStatus(EquipmentUnavailabilityStatus.UNDER_MAINTENANCE);
        maintenance.setStartTime(Instant.parse("2026-11-25T13:00:00Z"));
        maintenance.setEndTime(Instant.parse("2026-11-25T16:00:00Z"));
        when(equipmentUnavailabilityRepository.findByStartTimeLessThanAndEndTimeGreaterThan(END, START)).thenReturn(List.of(maintenance));

        List<ResourceCalendarResponse> calendar = service.getCalendar(START, END);

        assertEquals(List.of("w1", "w2", "e1"), calendar.stream().map(ResourceCalendarResponse::getId).toList());
        assertFalse(calendar.get(0).isActive());
        assertFalse(entriesOf(calendar, "w1").get(0).isConflict());
        assertTrue(entriesOf(calendar, "w2").isEmpty());
        ResourceCalendarResponse binRow = calendar.get(2);
        assertEquals("BN-1", binRow.getReference());
        assertEquals("BIN", binRow.getEquipmentType());
        assertEquals(List.of(EntryKind.TOUR, EntryKind.MAINTENANCE), binRow.getEntries().stream().map(Entry::getKind).toList());
        assertTrue(binRow.getEntries().stream().noneMatch(Entry::isConflict));
    }

    @Test
    void invalidPeriod_isRejectedBeforeReadingAnything() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.getCalendar(END, START));
        assertEquals("Start time must be before end time.", ex.getMessage());
        assertThrows(RuntimeException.class, () -> service.getCalendar(START, START));
        verifyNoInteractions(tourRepository, workerUnavailabilityRepository, equipmentUnavailabilityRepository);
    }
}
