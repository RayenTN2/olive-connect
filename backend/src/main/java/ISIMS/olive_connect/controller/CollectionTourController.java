package ISIMS.olive_connect.controller;

import ISIMS.olive_connect.dto.AssignTourResourcesRequest;
import ISIMS.olive_connect.dto.AvailableTourResourcesResponse;
import ISIMS.olive_connect.dto.CancelTourRequest;
import ISIMS.olive_connect.dto.CollectionTourResponse;
import ISIMS.olive_connect.dto.CreateCollectionTourRequest;
import ISIMS.olive_connect.dto.UpdateTourDropOffsRequest;
import ISIMS.olive_connect.dto.UpdateTourStopsRequest;
import ISIMS.olive_connect.dto.UpdateTourTeamLeaderRequest;
import ISIMS.olive_connect.entity.CollectionTourStatus;
import ISIMS.olive_connect.service.CollectionTourService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/collection-tours")
@PreAuthorize("hasAnyRole('COLLECTION_MANAGER', 'ADMIN')")
public class CollectionTourController {

    private final CollectionTourService tourService;

    public CollectionTourController(CollectionTourService tourService) {
        this.tourService = tourService;
    }

    @PostMapping
    public ResponseEntity<CollectionTourResponse> createTour(
            @Valid @RequestBody CreateCollectionTourRequest request,
            Authentication authentication) {
        String actor = authentication != null ? authentication.getName() : "system";
        return ResponseEntity.status(HttpStatus.CREATED).body(tourService.createTour(request, actor));
    }

    @GetMapping
    public ResponseEntity<List<CollectionTourResponse>> getTours(
            @RequestParam(required = false) CollectionTourStatus status) {
        return ResponseEntity.ok(tourService.getTours(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CollectionTourResponse> getTourById(@PathVariable String id) {
        return ResponseEntity.ok(tourService.getTourById(id));
    }

    @PutMapping("/{id}/stops")
    public ResponseEntity<CollectionTourResponse> updateStops(
            @PathVariable String id,
            @Valid @RequestBody UpdateTourStopsRequest request) {
        return ResponseEntity.ok(tourService.updateStops(id, request));
    }

    @PutMapping("/{id}/drop-offs")
    public ResponseEntity<CollectionTourResponse> updateDropOffs(
            @PathVariable String id,
            @Valid @RequestBody UpdateTourDropOffsRequest request) {
        return ResponseEntity.ok(tourService.updateDropOffs(id, request));
    }

    @PutMapping("/{id}/resources")
    public ResponseEntity<CollectionTourResponse> assignResources(
            @PathVariable String id,
            @Valid @RequestBody AssignTourResourcesRequest request) {
        return ResponseEntity.ok(tourService.assignResources(id, request));
    }

    @PatchMapping("/{id}/team-leader")
    public ResponseEntity<CollectionTourResponse> updateTeamLeader(
            @PathVariable String id,
            @Valid @RequestBody UpdateTourTeamLeaderRequest request) {
        return ResponseEntity.ok(tourService.updateTeamLeader(id, request));
    }

    @GetMapping("/{id}/available-resources")
    public ResponseEntity<AvailableTourResourcesResponse> getAvailableResources(@PathVariable String id) {
        return ResponseEntity.ok(tourService.getAvailableResourcesForTour(id));
    }

    @PostMapping("/{id}/recalculate")
    public ResponseEntity<CollectionTourResponse> recalculateTour(@PathVariable String id) {
        return ResponseEntity.ok(tourService.recalculateTour(id));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<CollectionTourResponse> confirmTour(@PathVariable String id) {
        return ResponseEntity.ok(tourService.confirmTour(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDraft(@PathVariable String id) {
        tourService.deleteDraft(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<CollectionTourResponse> cancelTour(
            @PathVariable String id,
            @Valid @RequestBody(required = false) CancelTourRequest request) {
        return ResponseEntity.ok(tourService.cancelTour(id, request));
    }
}