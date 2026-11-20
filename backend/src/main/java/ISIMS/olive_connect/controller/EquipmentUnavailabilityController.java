package ISIMS.olive_connect.controller;

import ISIMS.olive_connect.dto.EquipmentStatusResponse;
import ISIMS.olive_connect.dto.EquipmentUnavailabilityRequest;
import ISIMS.olive_connect.dto.EquipmentUnavailabilityResponse;
import ISIMS.olive_connect.service.EquipmentAvailabilityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/logistics/equipment")
@PreAuthorize("hasRole('LOGISTICS_MANAGER')")
public class EquipmentUnavailabilityController {

    private final EquipmentAvailabilityService availabilityService;

    public EquipmentUnavailabilityController(EquipmentAvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    // US-25: Mark equipment as under maintenance or unavailable for a period
    @PostMapping("/{equipmentId}/unavailabilities")
    public ResponseEntity<EquipmentUnavailabilityResponse> addUnavailability(
            @PathVariable String equipmentId,
            @Valid @RequestBody EquipmentUnavailabilityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(availabilityService.addUnavailability(equipmentId, request));
    }

    // US-25: List an item's maintenance/unavailability periods
    @GetMapping("/{equipmentId}/unavailabilities")
    public ResponseEntity<List<EquipmentUnavailabilityResponse>> getUnavailabilities(@PathVariable String equipmentId) {
        return ResponseEntity.ok(availabilityService.getUnavailabilities(equipmentId));
    }

    // US-25: Edit a period
    @PutMapping("/{equipmentId}/unavailabilities/{unavailabilityId}")
    public ResponseEntity<EquipmentUnavailabilityResponse> updateUnavailability(
            @PathVariable String equipmentId,
            @PathVariable String unavailabilityId,
            @Valid @RequestBody EquipmentUnavailabilityRequest request) {
        return ResponseEntity.ok(availabilityService.updateUnavailability(equipmentId, unavailabilityId, request));
    }

    // US-25: Delete a period (equipment becomes available again)
    @DeleteMapping("/{equipmentId}/unavailabilities/{unavailabilityId}")
    public ResponseEntity<Void> deleteUnavailability(
            @PathVariable String equipmentId,
            @PathVariable String unavailabilityId) {
        availabilityService.deleteUnavailability(equipmentId, unavailabilityId);
        return ResponseEntity.noContent().build();
    }

    // US-25: Consult the current (computed) equipment availability.
    // Not "/status": PATCH /{id}/status already means active/inactive (US-24).
    @GetMapping("/{equipmentId}/availability")
    public ResponseEntity<EquipmentStatusResponse> getCurrentStatus(@PathVariable String equipmentId) {
        return ResponseEntity.ok(availabilityService.getCurrentStatus(equipmentId));
    }
}
