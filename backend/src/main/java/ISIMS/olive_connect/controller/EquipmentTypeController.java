package ISIMS.olive_connect.controller;

import ISIMS.olive_connect.dto.CreateEquipmentTypeRequest;
import ISIMS.olive_connect.dto.EquipmentTypeResponse;
import ISIMS.olive_connect.service.EquipmentTypeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/logistics/equipment/types")
@PreAuthorize("hasRole('LOGISTICS_MANAGER')")
public class EquipmentTypeController {

    private final EquipmentTypeService equipmentTypeService;

    public EquipmentTypeController(EquipmentTypeService equipmentTypeService) {
        this.equipmentTypeService = equipmentTypeService;
    }

    // US-24: List built-in and custom equipment types, with their type-specific fields
    @GetMapping
    public ResponseEntity<List<EquipmentTypeResponse>> getTypes() {
        return ResponseEntity.ok(equipmentTypeService.getTypes());
    }

    // US-24: Add a custom equipment type
    @PostMapping
    public ResponseEntity<EquipmentTypeResponse> createType(@Valid @RequestBody CreateEquipmentTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(equipmentTypeService.createType(request));
    }
}
