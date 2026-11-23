package ISIMS.olive_connect.controller;

import ISIMS.olive_connect.dto.EquipmentRequest;
import ISIMS.olive_connect.dto.EquipmentResponse;
import ISIMS.olive_connect.dto.UpdateEquipmentStatusRequest;
import ISIMS.olive_connect.service.EquipmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/logistics/equipment")
@PreAuthorize("hasAnyRole('LOGISTICS_MANAGER', 'ADMIN')")
public class EquipmentController {

    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    // US-24: Register equipment
    @PostMapping
    public ResponseEntity<EquipmentResponse> createEquipment(@Valid @RequestBody EquipmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(equipmentService.createEquipment(request));
    }

    // US-24: List equipment (optionally filtered by ?type=<type code> and/or ?active=true|false)
    @GetMapping
    public ResponseEntity<List<EquipmentResponse>> getEquipment(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(equipmentService.getEquipment(type, active));
    }

    // US-24: Consult one equipment item
    @GetMapping("/{id}")
    public ResponseEntity<EquipmentResponse> getEquipmentById(@PathVariable String id) {
        return ResponseEntity.ok(equipmentService.getEquipmentById(id));
    }

    // US-24: Update equipment details
    @PutMapping("/{id}")
    public ResponseEntity<EquipmentResponse> updateEquipment(
            @PathVariable String id,
            @Valid @RequestBody EquipmentRequest request) {
        return ResponseEntity.ok(equipmentService.updateEquipment(id, request));
    }

    // US-24: Activate or deactivate equipment
    @PatchMapping("/{id}/status")
    public ResponseEntity<EquipmentResponse> updateEquipmentStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateEquipmentStatusRequest request) {
        return ResponseEntity.ok(equipmentService.updateEquipmentStatus(id, request.getActive()));
    }
}
