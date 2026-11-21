package ISIMS.olive_connect.controller;

import ISIMS.olive_connect.dto.UpdateWorkerStatusRequest;
import ISIMS.olive_connect.dto.WorkerRequest;
import ISIMS.olive_connect.dto.WorkerResponse;
import ISIMS.olive_connect.service.WorkerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/logistics/workers")
@PreAuthorize("hasAnyRole('COLLECTION_MANAGER', 'LOGISTICS_MANAGER', 'ADMIN')")
public class WorkerController {

    private final WorkerService workerService;

    public WorkerController(WorkerService workerService) {
        this.workerService = workerService;
    }

    // US-22: Register a seasonal worker
    @PostMapping
    @PreAuthorize("hasAnyRole('LOGISTICS_MANAGER', 'ADMIN')")
    public ResponseEntity<WorkerResponse> createWorker(@Valid @RequestBody WorkerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workerService.createWorker(request));
    }

    // US-22: List workers (optionally filtered by ?active=true|false)
    @GetMapping
    public ResponseEntity<List<WorkerResponse>> getWorkers(@RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(workerService.getWorkers(active));
    }

    // US-22: Update worker information
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('LOGISTICS_MANAGER', 'ADMIN')")
    public ResponseEntity<WorkerResponse> updateWorker(
            @PathVariable String id,
            @Valid @RequestBody WorkerRequest request) {
        return ResponseEntity.ok(workerService.updateWorker(id, request));
    }

    // US-22: Activate or deactivate a worker
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('LOGISTICS_MANAGER', 'ADMIN')")
    public ResponseEntity<WorkerResponse> updateWorkerStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateWorkerStatusRequest request) {
        return ResponseEntity.ok(workerService.updateWorkerStatus(id, request.getActive()));
    }
}