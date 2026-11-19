package ISIMS.olive_connect.controller;

import ISIMS.olive_connect.dto.CreateHarvestRequestDto;
import ISIMS.olive_connect.dto.HarvestRequestResponse;
import ISIMS.olive_connect.service.HarvestRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/harvest-requests")
public class HarvestRequestController {

    private final HarvestRequestService harvestRequestService;

    public HarvestRequestController(HarvestRequestService harvestRequestService) {
        this.harvestRequestService = harvestRequestService;
    }

    @PostMapping
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<HarvestRequestResponse> submitHarvestRequest(
            @Valid @RequestBody CreateHarvestRequestDto request,
            Authentication authentication) {
        HarvestRequestResponse response = harvestRequestService.createHarvestRequest(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my-requests")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<List<HarvestRequestResponse>> getMyHarvestRequests(Authentication authentication) {
        return ResponseEntity.ok(harvestRequestService.getMyHarvestRequests(authentication.getName()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HarvestRequestResponse> getHarvestRequestById(
        @PathVariable String id,
        Authentication authentication) {
        return ResponseEntity.ok(harvestRequestService.getHarvestRequestById(id, authentication.getName()));
    }
}