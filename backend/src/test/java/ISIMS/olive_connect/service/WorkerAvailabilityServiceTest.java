package ISIMS.olive_connect.service;

import ISIMS.olive_connect.dto.WorkerResponse;
import ISIMS.olive_connect.dto.WorkerUnavailabilityRequest;
import ISIMS.olive_connect.dto.WorkerUnavailabilityResponse;
import ISIMS.olive_connect.entity.Worker;
import ISIMS.olive_connect.entity.WorkerUnavailability;
import ISIMS.olive_connect.exception.ResourceNotFoundException;
import ISIMS.olive_connect.repository.WorkerRepository;
import ISIMS.olive_connect.repository.WorkerUnavailabilityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkerAvailabilityServiceTest {

    private static final Instant T10 = Instant.parse("2026-11-20T10:00:00Z");
    private static final Instant T12 = Instant.parse("2026-11-20T12:00:00Z");
    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private WorkerUnavailabilityRepository unavailabilityRepository;

    @Mock
    private WorkerRepository workerRepository;

    @Mock
    private WorkerService workerService;

    @InjectMocks
    private WorkerAvailabilityService service;

    @BeforeEach
    void setUp() {
        lenient().when(unavailabilityRepository.save(any(WorkerUnavailability.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private static WorkerUnavailabilityRequest request(Instant start, Instant end) {
        WorkerUnavailabilityRequest request = new WorkerUnavailabilityRequest();
        request.setStartTime(start);
        request.setEndTime(end);
        request.setReason("Congé");
        return request;
    }

    private static Worker worker(String id, boolean active) {
        Worker worker = new Worker();
        worker.setId(id);
        worker.setActive(active);
        return worker;
    }

    private static WorkerUnavailability interval(String id, String workerId) {
        WorkerUnavailability interval = new WorkerUnavailability();
        interval.setId(id);
        interval.setWorkerId(workerId);
        interval.setStartTime(T10);
        interval.setEndTime(T12);
        interval.setReason("Absence");
        interval.setCreatedAt(CREATED);
        interval.setUpdatedAt(CREATED);
        return interval;
    }

    // --- create ---

    @Test
    void addUnavailability_savesIntervalForWorker() {
        WorkerUnavailabilityResponse response = service.addUnavailability("w1", request(T10, T12));

        verify(workerService).findWorker("w1");
        assertEquals("w1", response.getWorkerId());
        assertEquals(T10, response.getStartTime());
        assertEquals(T12, response.getEndTime());
        assertEquals("Congé", response.getReason());
        assertNotNull(response.getCreatedAt());
    }

    @Test
    void addUnavailability_startEqualsEnd_isRejected() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.addUnavailability("w1", request(T10, T10)));
        assertEquals("Start time must be before end time.", ex.getMessage());
        verify(unavailabilityRepository, never()).save(any());
    }

    @Test
    void addUnavailability_startAfterEnd_isRejected() {
        assertThrows(RuntimeException.class, () -> service.addUnavailability("w1", request(T12, T10)));
        verify(unavailabilityRepository, never()).save(any());
    }

    @Test
    void addUnavailability_unknownWorker_isNotFound() {
        when(workerService.findWorker("missing")).thenThrow(new ResourceNotFoundException("Worker not found with id: missing"));

        assertThrows(ResourceNotFoundException.class, () -> service.addUnavailability("missing", request(T10, T12)));
        verify(unavailabilityRepository, never()).save(any());
    }

    // --- list ---

    @Test
    void getUnavailabilities_returnsWorkerIntervals() {
        when(unavailabilityRepository.findByWorkerIdOrderByStartTimeAsc("w1"))
                .thenReturn(List.of(interval("u1", "w1"), interval("u2", "w1")));

        assertEquals(2, service.getUnavailabilities("w1").size());
    }

    // --- edit ---

    @Test
    void updateUnavailability_changesFieldsAndKeepsCreatedAt() {
        when(unavailabilityRepository.findByIdAndWorkerId("u1", "w1")).thenReturn(Optional.of(interval("u1", "w1")));
        Instant newEnd = Instant.parse("2026-11-21T12:00:00Z");

        WorkerUnavailabilityResponse response = service.updateUnavailability("w1", "u1", request(T10, newEnd));

        assertEquals(newEnd, response.getEndTime());
        assertEquals("Congé", response.getReason());
        assertEquals(CREATED, response.getCreatedAt());
        assertTrue(response.getUpdatedAt().isAfter(CREATED));
    }

    @Test
    void updateUnavailability_intervalOfAnotherWorker_isNotFound() {
        when(unavailabilityRepository.findByIdAndWorkerId("u1", "w2")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateUnavailability("w2", "u1", request(T10, T12)));
        verify(unavailabilityRepository, never()).save(any());
    }

    @Test
    void updateUnavailability_invalidPeriod_isRejected() {
        when(unavailabilityRepository.findByIdAndWorkerId("u1", "w1")).thenReturn(Optional.of(interval("u1", "w1")));

        assertThrows(RuntimeException.class, () -> service.updateUnavailability("w1", "u1", request(T12, T10)));
        verify(unavailabilityRepository, never()).save(any());
    }

    // --- delete ---

    @Test
    void deleteUnavailability_deletesTheInterval() {
        WorkerUnavailability interval = interval("u1", "w1");
        when(unavailabilityRepository.findByIdAndWorkerId("u1", "w1")).thenReturn(Optional.of(interval));

        service.deleteUnavailability("w1", "u1");

        verify(unavailabilityRepository).delete(interval);
    }

    @Test
    void deleteUnavailability_unknownInterval_isNotFound() {
        when(unavailabilityRepository.findByIdAndWorkerId("missing", "w1")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteUnavailability("w1", "missing"));
        verify(unavailabilityRepository, never()).delete(any());
    }

    // --- unavailable / assignable workers ---

    @Test
    @SuppressWarnings("unchecked")
    void getUnavailableWorkers_passesEndThenStart_andListsEachWorkerOnce() {
        when(unavailabilityRepository.findByStartTimeLessThanAndEndTimeGreaterThan(T12, T10))
                .thenReturn(List.of(interval("u1", "w1"), interval("u2", "w1")));
        when(workerRepository.findAllById(any())).thenReturn(List.of(worker("w1", true)));

        List<WorkerResponse> result = service.getUnavailableWorkers(T10, T12);

        ArgumentCaptor<Iterable<String>> ids = ArgumentCaptor.forClass(Iterable.class);
        verify(workerRepository).findAllById(ids.capture());
        assertEquals(Set.of("w1"), ids.getValue());
        assertEquals(1, result.size());
    }

    @Test
    void getUnavailableWorkers_invalidPeriod_isRejected() {
        assertThrows(RuntimeException.class, () -> service.getUnavailableWorkers(T12, T10));
        verifyNoInteractions(unavailabilityRepository);
    }

    @Test
    void getAssignableWorkers_returnsOnlyActiveWorkersWithoutOverlap() {
        when(unavailabilityRepository.findByStartTimeLessThanAndEndTimeGreaterThan(T12, T10))
                .thenReturn(List.of(interval("u1", "busy")));
        when(workerRepository.findByActive(true)).thenReturn(List.of(worker("free", true), worker("busy", true)));

        List<WorkerResponse> result = service.getAssignableWorkers(T10, T12);

        assertEquals(List.of("free"), result.stream().map(WorkerResponse::getId).toList());
        // Inactive workers are never even loaded
        verify(workerRepository, never()).findByActive(false);
        verify(workerRepository, never()).findAll();
    }
}
