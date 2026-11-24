package ISIMS.olive_connect.controller;

import ISIMS.olive_connect.dto.EquipmentResponse;
import ISIMS.olive_connect.dto.WorkerResponse;
import ISIMS.olive_connect.service.CollectionTourService;
import ISIMS.olive_connect.service.EquipmentTypeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * US-26: Resources the Collection Manager can assign during a proposed planning period (before a tour exists).
 * Same rule as /api/collection-tours/{id}/available-resources: unavailability periods (US-23/US-25)
 * and confirmed tours, both computed by CollectionTourService.
 */
@RestController
@RequestMapping("/api/collection/resources")
@PreAuthorize("hasAnyRole('COLLECTION_MANAGER', 'ADMIN')")
public class ResourceAvailabilityController {

    private final CollectionTourService tourService;
    private final EquipmentTypeService equipmentTypeService;

    public ResourceAvailabilityController(CollectionTourService tourService,
                                          EquipmentTypeService equipmentTypeService) {
        this.tourService = tourService;
        this.equipmentTypeService = equipmentTypeService;
    }

    // US-26: Active workers free during the whole period, e.g. ?start=2026-11-25T08:00:00Z&end=2026-11-25T17:00:00Z
    @GetMapping("/workers")
    public ResponseEntity<List<WorkerResponse>> getAvailableWorkers(
            @RequestParam Instant start,
            @RequestParam Instant end) {
        return ResponseEntity.ok(tourService.getAvailableWorkers(start, end, null));
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
        List<EquipmentResponse> equipment = tourService.getAvailableEquipment(start, end, null);
        return ResponseEntity.ok(type == null
                ? equipment
                : equipment.stream().filter(item -> type.equals(item.getType())).toList());
    }
}
