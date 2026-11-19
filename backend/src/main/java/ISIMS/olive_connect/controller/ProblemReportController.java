package ISIMS.olive_connect.controller;

import ISIMS.olive_connect.dto.ProblemReportResponse;
import ISIMS.olive_connect.dto.ReportProblemRequest;
import ISIMS.olive_connect.dto.UpdateProblemStatusRequest;
import ISIMS.olive_connect.entity.ProblemStatus;
import ISIMS.olive_connect.entity.ProblemType;
import ISIMS.olive_connect.service.ProblemReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/problem-reports")
public class ProblemReportController {

    private final ProblemReportService problemReportService;

    public ProblemReportController(ProblemReportService problemReportService) {
        this.problemReportService = problemReportService;
    }

    @PostMapping
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<ProblemReportResponse> reportProblem(
            @Valid @RequestBody ReportProblemRequest request,
            Authentication authentication) {
        ProblemReportResponse response = problemReportService.reportProblem(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my-reports")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<List<ProblemReportResponse>> getMyReports(Authentication authentication) {
        return ResponseEntity.ok(problemReportService.getMyReports(authentication.getName()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('COLLECTION_MANAGER', 'ADMIN')")
    public ResponseEntity<List<ProblemReportResponse>> getAllReports(
            @RequestParam(required = false) ProblemType problemType,
            @RequestParam(required = false) ProblemStatus status,
            @RequestParam(required = false) String orchardId) {
        return ResponseEntity.ok(problemReportService.getReportsForManager(problemType, status, orchardId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProblemReportResponse> getReportById(
            @PathVariable String id,
            Authentication authentication) {
        return ResponseEntity.ok(problemReportService.getReportById(id, authentication.getName()));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('COLLECTION_MANAGER', 'ADMIN')")
    public ResponseEntity<ProblemReportResponse> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateProblemStatusRequest request) {
        return ResponseEntity.ok(problemReportService.updateReportStatus(id, request.getStatus()));
    }
}