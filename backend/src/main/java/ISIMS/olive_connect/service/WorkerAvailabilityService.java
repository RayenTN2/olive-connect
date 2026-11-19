package ISIMS.olive_connect.service;

import ISIMS.olive_connect.dto.WorkerResponse;
import ISIMS.olive_connect.dto.WorkerUnavailabilityRequest;
import ISIMS.olive_connect.dto.WorkerUnavailabilityResponse;
import ISIMS.olive_connect.entity.WorkerUnavailability;
import ISIMS.olive_connect.exception.ResourceNotFoundException;
import ISIMS.olive_connect.repository.WorkerRepository;
import ISIMS.olive_connect.repository.WorkerUnavailabilityRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * US-23: Worker availability rules, shared by every caller (Logistics Manager now, tour planning in #26/#35/#42).
 * Tour planning must use {@link #getAssignableWorkers} so inactive or unavailable workers are never offered.
 */
@Service
public class WorkerAvailabilityService {

    private final WorkerUnavailabilityRepository unavailabilityRepository;
    private final WorkerRepository workerRepository;
    private final WorkerService workerService;

    public WorkerAvailabilityService(WorkerUnavailabilityRepository unavailabilityRepository,
                                     WorkerRepository workerRepository,
                                     WorkerService workerService) {
        this.unavailabilityRepository = unavailabilityRepository;
        this.workerRepository = workerRepository;
        this.workerService = workerService;
    }

    // 1. Register an unavailability interval for a worker (US-23)
    public WorkerUnavailabilityResponse addUnavailability(String workerId, WorkerUnavailabilityRequest request) {
        workerService.findWorker(workerId);
        validatePeriod(request.getStartTime(), request.getEndTime());

        Instant now = Instant.now();
        WorkerUnavailability unavailability = new WorkerUnavailability();
        unavailability.setWorkerId(workerId);
        unavailability.setStartTime(request.getStartTime());
        unavailability.setEndTime(request.getEndTime());
        unavailability.setReason(request.getReason());
        unavailability.setCreatedAt(now);
        unavailability.setUpdatedAt(now);

        return WorkerUnavailabilityResponse.fromEntity(unavailabilityRepository.save(unavailability));
    }

    // 2. List a worker's unavailability intervals, chronologically (US-23)
    public List<WorkerUnavailabilityResponse> getUnavailabilities(String workerId) {
        workerService.findWorker(workerId);

        return unavailabilityRepository.findByWorkerIdOrderByStartTimeAsc(workerId)
                .stream()
                .map(WorkerUnavailabilityResponse::fromEntity)
                .toList();
    }

    // 3. Edit an interval, e.g. to fix a mistake (US-23)
    public WorkerUnavailabilityResponse updateUnavailability(String workerId, String unavailabilityId,
                                                             WorkerUnavailabilityRequest request) {
        WorkerUnavailability unavailability = findUnavailability(workerId, unavailabilityId);
        validatePeriod(request.getStartTime(), request.getEndTime());

        unavailability.setStartTime(request.getStartTime());
        unavailability.setEndTime(request.getEndTime());
        unavailability.setReason(request.getReason());
        unavailability.setUpdatedAt(Instant.now());

        return WorkerUnavailabilityResponse.fromEntity(unavailabilityRepository.save(unavailability));
    }

    // 4. Delete an interval (US-23). Intervals are planning data, not tour history, so a hard delete is fine.
    public void deleteUnavailability(String workerId, String unavailabilityId) {
        unavailabilityRepository.delete(findUnavailability(workerId, unavailabilityId));
    }

    // 5. Workers having at least one unavailability overlapping [start, end), each listed once (US-23)
    public List<WorkerResponse> getUnavailableWorkers(Instant start, Instant end) {
        validatePeriod(start, end);

        return workerRepository.findAllById(unavailableWorkerIds(start, end))
                .stream()
                .map(WorkerResponse::fromEntity)
                .toList();
    }

    // 6. Workers that can be assigned during [start, end): active AND not unavailable.
    // Single source of truth for tour planning (#26, #35, #42).
    public List<WorkerResponse> getAssignableWorkers(Instant start, Instant end) {
        validatePeriod(start, end);
        Set<String> unavailableIds = unavailableWorkerIds(start, end);

        return workerRepository.findByActive(true)
                .stream()
                .filter(worker -> !unavailableIds.contains(worker.getId()))
                .map(WorkerResponse::fromEntity)
                .toList();
    }

    private Set<String> unavailableWorkerIds(Instant start, Instant end) {
        // Argument order matters: (periodEnd, periodStart) — see the repository method
        return unavailabilityRepository.findByStartTimeLessThanAndEndTimeGreaterThan(end, start)
                .stream()
                .map(WorkerUnavailability::getWorkerId)
                .collect(Collectors.toSet());
    }

    private WorkerUnavailability findUnavailability(String workerId, String unavailabilityId) {
        workerService.findWorker(workerId);
        return unavailabilityRepository.findByIdAndWorkerId(unavailabilityId, workerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Unavailability not found with id: " + unavailabilityId + " for worker: " + workerId));
    }

    private void validatePeriod(Instant start, Instant end) {
        if (start == null || end == null) {
            throw new RuntimeException("Start time and end time are required.");
        }
        if (!start.isBefore(end)) {
            throw new RuntimeException("Start time must be before end time.");
        }
    }
}
