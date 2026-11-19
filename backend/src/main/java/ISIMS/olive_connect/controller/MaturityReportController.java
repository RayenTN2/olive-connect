package ISIMS.olive_connect.controller;

import ISIMS.olive_connect.dto.MaturityReportResponse;
import ISIMS.olive_connect.dto.ReportMaturityRequest;
import ISIMS.olive_connect.entity.MaturityLevel;
import ISIMS.olive_connect.service.MaturityReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/maturity-reports")
public class MaturityReportController {

    private final MaturityReportService maturityReportService;

    public MaturityReportController(MaturityReportService maturityReportService) {
        this.maturityReportService = maturityReportService;
    }

    // Farmer submits a maturity level report for their orchard
    @PostMapping
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<MaturityReportResponse> reportMaturity(
            @Valid @RequestBody ReportMaturityRequest request,
            Authentication authentication) {
        MaturityReportResponse response = maturityReportService.reportMaturity(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Farmer consults their own submitted maturity reports
    @GetMapping("/my-reports")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<List<MaturityReportResponse>> getMyReports(Authentication authentication) {
        return ResponseEntity.ok(maturityReportService.getMyReports(authentication.getName()));
    }

    // Collection Manager consults submitted maturity reports
    @GetMapping
    @PreAuthorize("hasAnyRole('COLLECTION_MANAGER', 'ADMIN')")
    public ResponseEntity<List<MaturityReportResponse>> getAllReports(
            @RequestParam(required = false) String orchardId,
            @RequestParam(required = false) MaturityLevel maturityLevel) {
        return ResponseEntity.ok(maturityReportService.getReportsForManager(orchardId, maturityLevel));
    }

    // Maturity report history for a specific orchard
    @GetMapping("/orchard/{orchardId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MaturityReportResponse>> getOrchardHistory(
            @PathVariable String orchardId,
            Authentication authentication) {
        return ResponseEntity.ok(maturityReportService.getOrchardMaturityHistory(orchardId, authentication.getName()));
    }
}