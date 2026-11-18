package ISIMS.olive_connect.controller;

import ISIMS.olive_connect.dto.OrchardResponse;
import ISIMS.olive_connect.dto.RegisterOrchardRequest;
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
}