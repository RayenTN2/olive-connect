package ISIMS.olive_connect.service;

import ISIMS.olive_connect.dto.WorkerRequest;
import ISIMS.olive_connect.dto.WorkerResponse;
import ISIMS.olive_connect.entity.Worker;
import ISIMS.olive_connect.exception.ResourceNotFoundException;
import ISIMS.olive_connect.repository.WorkerRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class WorkerService {

    private final WorkerRepository workerRepository;

    public WorkerService(WorkerRepository workerRepository) {
        this.workerRepository = workerRepository;
    }

    // 1. Register a new seasonal worker, active by default (US-22)
    public WorkerResponse createWorker(WorkerRequest request) {
        Instant now = Instant.now();
        Worker worker = new Worker();
        worker.setFullName(request.getFullName());
        worker.setPhone(request.getPhone());
        worker.setActive(true);
        worker.setCreatedAt(now);
        worker.setUpdatedAt(now);

        return WorkerResponse.fromEntity(workerRepository.save(worker));
    }

    // 2. List workers, optionally filtered by active status (US-22)
    public List<WorkerResponse> getWorkers(Boolean active) {
        List<Worker> workers = (active != null)
                ? workerRepository.findByActive(active)
                : workerRepository.findAll();

        return workers.stream()
                .map(WorkerResponse::fromEntity)
                .toList();
    }

    // 3. Update a worker's information (US-22)
    public WorkerResponse updateWorker(String workerId, WorkerRequest request) {
        Worker worker = findWorker(workerId);
        worker.setFullName(request.getFullName());
        worker.setPhone(request.getPhone());
        worker.setUpdatedAt(Instant.now());

        return WorkerResponse.fromEntity(workerRepository.save(worker));
    }

    // 4. Activate or deactivate a worker (US-22).
    // Deactivation never deletes the record, so past tours keep a valid worker reference.
    public WorkerResponse updateWorkerStatus(String workerId, boolean active) {
        Worker worker = findWorker(workerId);
        worker.setActive(active);
        worker.setUpdatedAt(Instant.now());

        return WorkerResponse.fromEntity(workerRepository.save(worker));
    }

    // Shared with WorkerAvailabilityService: unknown id → 404
    public Worker findWorker(String workerId) {
        return workerRepository.findById(workerId)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found with id: " + workerId));
    }
}
