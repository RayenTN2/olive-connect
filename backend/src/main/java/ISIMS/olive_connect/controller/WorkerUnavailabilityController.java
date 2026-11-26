package ISIMS.olive_connect.controller;

import ISIMS.olive_connect.dto.WorkerResponse;
import ISIMS.olive_connect.dto.WorkerUnavailabilityRequest;
import ISIMS.olive_connect.dto.WorkerUnavailabilityResponse;
import ISIMS.olive_connect.service.WorkerAvailabilityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/logistics/workers")
@PreAuthorize("hasAnyRole('LOGISTICS_MANAGER', 'ADMIN')")
public class WorkerUnavailabilityController {

    private final WorkerAvailabilityService availabilityService;

    public WorkerUnavailabilityController(WorkerAvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    // US-23: Register an unavailability interval for a worker
    @PostMapping("/{workerId}/unavailabilities")
    public ResponseEntity<WorkerUnavailabilityResponse> addUnavailability(
            @PathVariable String workerId,
            @Valid @RequestBody WorkerUnavailabilityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(availabilityService.addUnavailability(workerId, request));
    }

    // US-23: List a worker's unavailability intervals
    @GetMapping("/{workerId}/unavailabilities")
    public ResponseEntity<List<WorkerUnavailabilityResponse>> getUnavailabilities(@PathVariable String workerId) {
        return ResponseEntity.ok(availabilityService.getUnavailabilities(workerId));
    }

    // US-23: Edit an unavailability interval
    @PutMapping("/{workerId}/unavailabilities/{unavailabilityId}")
    public ResponseEntity<WorkerUnavailabilityResponse> updateUnavailability(
            @PathVariable String workerId,
            @PathVariable String unavailabilityId,
            @Valid @RequestBody WorkerUnavailabilityRequest request) {
        return ResponseEntity.ok(availabilityService.updateUnavailability(workerId, unavailabilityId, request));
    }

    // US-23: Delete an unavailability interval
    @DeleteMapping("/{workerId}/unavailabilities/{unavailabilityId}")
    public ResponseEntity<Void> deleteUnavailability(
            @PathVariable String workerId,
            @PathVariable String unavailabilityId) {
        availabilityService.deleteUnavailability(workerId, unavailabilityId);
        return ResponseEntity.noContent().build();
    }

    // US-23: Workers unavailable during a proposed tour period, e.g. ?start=2026-11-20T08:00:00Z&end=2026-11-20T18:00:00Z
    @GetMapping("/unavailable")
    public ResponseEntity<List<WorkerResponse>> getUnavailableWorkers(
            @RequestParam Instant start,
            @RequestParam Instant end) {
        return ResponseEntity.ok(availabilityService.getUnavailableWorkers(start, end));
    }
}
