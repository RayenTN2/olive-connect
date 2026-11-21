package ISIMS.olive_connect.controller;

import ISIMS.olive_connect.dto.OilMillRequest;
import ISIMS.olive_connect.dto.OilMillResponse;
import ISIMS.olive_connect.service.OilMillService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/oil-mills")
public class OilMillController {

    private final OilMillService oilMillService;

    public OilMillController(OilMillService oilMillService) {
        this.oilMillService = oilMillService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('COLLECTION_MANAGER', 'LOGISTICS_MANAGER', 'ADMIN')")
    public ResponseEntity<OilMillResponse> createOilMill(@Valid @RequestBody OilMillRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(oilMillService.createOilMill(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('COLLECTION_MANAGER', 'LOGISTICS_MANAGER', 'ADMIN')")
    public ResponseEntity<List<OilMillResponse>> getOilMills(@RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(oilMillService.getOilMills(active));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('COLLECTION_MANAGER', 'LOGISTICS_MANAGER', 'ADMIN')")
    public ResponseEntity<OilMillResponse> getOilMillById(@PathVariable String id) {
        return ResponseEntity.ok(oilMillService.getOilMillById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('COLLECTION_MANAGER', 'LOGISTICS_MANAGER', 'ADMIN')")
    public ResponseEntity<OilMillResponse> updateOilMill(
            @PathVariable String id,
            @Valid @RequestBody OilMillRequest request) {
        return ResponseEntity.ok(oilMillService.updateOilMill(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('COLLECTION_MANAGER', 'LOGISTICS_MANAGER', 'ADMIN')")
    public ResponseEntity<OilMillResponse> updateOilMillStatus(
            @PathVariable String id,
            @RequestParam boolean active) {
        return ResponseEntity.ok(oilMillService.updateOilMillStatus(id, active));
    }
}