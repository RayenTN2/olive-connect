package ISIMS.olive_connect.controller;

import ISIMS.olive_connect.dto.OrchardGeoJsonCollection;
import ISIMS.olive_connect.dto.OrchardResponse;
import ISIMS.olive_connect.dto.RegisterOrchardRequest;
import ISIMS.olive_connect.dto.UpdateOrchardRequest;
import ISIMS.olive_connect.entity.MaturityLevel;
import ISIMS.olive_connect.entity.OrchardHarvestStatus;
import ISIMS.olive_connect.service.OrchardService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orchards")
public class OrchardController {

    private final OrchardService orchardService;

    public OrchardController(OrchardService orchardService) {
        this.orchardService = orchardService;
    }

    // US-06, US-07, US-08: Farmer registers a new orchard with characteristics and GPS location
    @PostMapping
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<OrchardResponse> registerOrchard(
            @Valid @RequestBody RegisterOrchardRequest request,
            Authentication authentication) {
        OrchardResponse response = orchardService.registerOrchard(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // List orchards belonging to the logged-in farmer
    @GetMapping("/my-orchards")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<List<OrchardResponse>> getMyOrchards(Authentication authentication) {
        List<OrchardResponse> orchards = orchardService.getFarmerOrchards(authentication.getName());
        return ResponseEntity.ok(orchards);
    }

    // US-09: View details of a specific orchard owned by the logged-in farmer
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<OrchardResponse> getOrchardById(
            @PathVariable String id,
            Authentication authentication) {
        OrchardResponse response = orchardService.getOrchardById(id, authentication.getName());
        return ResponseEntity.ok(response);
    }

    // US-09: Update orchard characteristics and coordinates
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<OrchardResponse> updateOrchard(
            @PathVariable String id,
            @Valid @RequestBody UpdateOrchardRequest request,
            Authentication authentication) {
        OrchardResponse response = orchardService.updateOrchard(id, request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    // US-10 (Public Visitor): View public orchard map without authentication
    @GetMapping("/map/public")
    public ResponseEntity<OrchardGeoJsonCollection> getPublicOrchardsForMap(
            @RequestParam(required = false) MaturityLevel maturity,
            @RequestParam(required = false) OrchardHarvestStatus harvestStatus) {
        OrchardGeoJsonCollection geoJson = orchardService.getPublicOrchardsMapGeoJson(maturity, harvestStatus);
        return ResponseEntity.ok(geoJson);
    }

    // US-10 (Authenticated): View orchards on an interactive map (role-scoped)
    @GetMapping("/map")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrchardGeoJsonCollection> getOrchardsForMap(
            @RequestParam(required = false) MaturityLevel maturity,
            @RequestParam(required = false) OrchardHarvestStatus harvestStatus,
            Authentication authentication) {
        OrchardGeoJsonCollection geoJson = orchardService.getOrchardsMapGeoJson(
                maturity, harvestStatus, authentication.getName());
        return ResponseEntity.ok(geoJson);
    }
}