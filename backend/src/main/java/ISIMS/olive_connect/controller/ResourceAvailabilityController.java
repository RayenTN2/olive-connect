package ISIMS.olive_connect.controller;

import ISIMS.olive_connect.dto.EquipmentResponse;
import ISIMS.olive_connect.dto.WorkerResponse;
import ISIMS.olive_connect.service.EquipmentAvailabilityService;
import ISIMS.olive_connect.service.EquipmentTypeService;
import ISIMS.olive_connect.service.WorkerAvailabilityService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * US-26: Resources the Collection Manager can assign during a proposed planning period.
 * No availability logic here: it reuses getAssignableWorkers (US-23) and getAssignableEquipment (US-25).
 * shortcut: confirmed tour assignments are not considered yet (no tours exist) — add them inside
 * those two service methods in #35/#36/#43 and these endpoints pick them up unchanged.
 */
@RestController
@RequestMapping("/api/collection/resources")
@PreAuthorize("hasRole('COLLECTION_MANAGER')")
public class ResourceAvailabilityController {

    private final WorkerAvailabilityService workerAvailabilityService;
    private final EquipmentAvailabilityService equipmentAvailabilityService;
    private final EquipmentTypeService equipmentTypeService;

    public ResourceAvailabilityController(WorkerAvailabilityService workerAvailabilityService,
                                          EquipmentAvailabilityService equipmentAvailabilityService,
                                          EquipmentTypeService equipmentTypeService) {
        this.workerAvailabilityService = workerAvailabilityService;
        this.equipmentAvailabilityService = equipmentAvailabilityService;
        this.equipmentTypeService = equipmentTypeService;
    }

    // US-26: Active workers free during the whole period, e.g. ?start=2026-11-25T08:00:00Z&end=2026-11-25T17:00:00Z
    @GetMapping("/workers")
    public ResponseEntity<List<WorkerResponse>> getAvailableWorkers(
            @RequestParam Instant start,
            @RequestParam Instant end) {
        return ResponseEntity.ok(workerAvailabilityService.getAssignableWorkers(start, end));
    }

    // US-26: Active equipment free during the whole period, optionally of one type (?type=BIN)
    @GetMapping("/equipment")
    public ResponseEntity<List<EquipmentResponse>> getAvailableEquipment(
            @RequestParam Instant start,
            @RequestParam Instant end,
            @RequestParam(required = false) String type) {
        if (type != null) {
            equipmentTypeService.requireKnownType(type);
        }
        List<EquipmentResponse> equipment = equipmentAvailabilityService.getAssignableEquipment(start, end);
        return ResponseEntity.ok(type == null
                ? equipment
                : equipment.stream().filter(item -> type.equals(item.getType())).toList());
    }
}
