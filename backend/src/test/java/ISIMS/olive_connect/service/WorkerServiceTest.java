package ISIMS.olive_connect.service;

import ISIMS.olive_connect.dto.WorkerRequest;
import ISIMS.olive_connect.dto.WorkerResponse;
import ISIMS.olive_connect.entity.Worker;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkerServiceTest {

    @Mock
    private WorkerRepository workerRepository;

    @InjectMocks
    private WorkerService workerService;

    private WorkerRequest request;

    @BeforeEach
    void setUp() {
        request = new WorkerRequest();
        request.setFullName("Ali Ben Salah");
        request.setPhone("+21698123456");
        // save() returns the entity it receives, like MongoDB would
        lenient().when(workerRepository.save(any(Worker.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Worker existingWorker(boolean active) {
        Worker worker = new Worker();
        worker.setId("w1");
        worker.setFullName("Old Name");
        worker.setPhone("000");
        worker.setActive(active);
        worker.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        worker.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        return worker;
    }

    @Test
    void createWorker_savesActiveWorkerWithTimestamps() {
        WorkerResponse response = workerService.createWorker(request);

        assertEquals("Ali Ben Salah", response.getFullName());
        assertEquals("+21698123456", response.getPhone());
        assertTrue(response.isActive());
        assertNotNull(response.getCreatedAt());
        assertEquals(response.getCreatedAt(), response.getUpdatedAt());
    }

    @Test
    void getWorkers_withFilter_usesFindByActive() {
        when(workerRepository.findByActive(true)).thenReturn(List.of(existingWorker(true)));

        assertEquals(1, workerService.getWorkers(true).size());
        verify(workerRepository, never()).findAll();
    }

    @Test
    void getWorkers_withoutFilter_returnsAll() {
        when(workerRepository.findAll()).thenReturn(List.of(existingWorker(true), existingWorker(false)));

        assertEquals(2, workerService.getWorkers(null).size());
        verify(workerRepository, never()).findByActive(anyBoolean());
    }

    @Test
    void updateWorker_changesInfoAndKeepsCreatedAt() {
        Worker worker = existingWorker(true);
        when(workerRepository.findById("w1")).thenReturn(Optional.of(worker));

        WorkerResponse response = workerService.updateWorker("w1", request);

        assertEquals("Ali Ben Salah", response.getFullName());
        assertEquals("+21698123456", response.getPhone());
        assertEquals(Instant.parse("2026-01-01T00:00:00Z"), response.getCreatedAt());
        assertTrue(response.getUpdatedAt().isAfter(response.getCreatedAt()));
    }

    @Test
    void updateWorker_unknownId_throws() {
        when(workerRepository.findById("missing")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> workerService.updateWorker("missing", request));
        assertEquals("Worker not found with id: missing", ex.getMessage());
    }

    @Test
    void deactivateWorker_keepsRecordInsteadOfDeleting() {
        when(workerRepository.findById("w1")).thenReturn(Optional.of(existingWorker(true)));

        WorkerResponse response = workerService.updateWorkerStatus("w1", false);

        assertFalse(response.isActive());
        assertEquals("w1", response.getId());
        verify(workerRepository).save(any(Worker.class));
        verify(workerRepository, never()).delete(any());
        verify(workerRepository, never()).deleteById(any());
    }

    @Test
    void reactivateWorker_setsActiveTrue() {
        when(workerRepository.findById("w1")).thenReturn(Optional.of(existingWorker(false)));

        assertTrue(workerService.updateWorkerStatus("w1", true).isActive());
    }

    @Test
    void updateWorkerStatus_unknownId_throws() {
        when(workerRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> workerService.updateWorkerStatus("missing", false));
    }
}
