package ISIMS.olive_connect.service;

import ISIMS.olive_connect.dto.MaturityReportResponse;
import ISIMS.olive_connect.dto.ReportMaturityRequest;
import ISIMS.olive_connect.entity.MaturityLevel;
import ISIMS.olive_connect.entity.MaturityReport;
import ISIMS.olive_connect.entity.Orchard;
import ISIMS.olive_connect.entity.Role;
import ISIMS.olive_connect.entity.User;
import ISIMS.olive_connect.repository.MaturityReportRepository;
import ISIMS.olive_connect.repository.OrchardRepository;
import ISIMS.olive_connect.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class MaturityReportService {

    private final MaturityReportRepository maturityReportRepository;
    private final OrchardRepository orchardRepository;
    private final UserRepository userRepository;

    public MaturityReportService(MaturityReportRepository maturityReportRepository,
                                 OrchardRepository orchardRepository,
                                 UserRepository userRepository) {
        this.maturityReportRepository = maturityReportRepository;
        this.orchardRepository = orchardRepository;
        this.userRepository = userRepository;
    }

    // Farmer reports maturity level for one of their owned orchards
    public MaturityReportResponse reportMaturity(ReportMaturityRequest request, String farmerEmail) {
        User farmer = userRepository.findByEmail(farmerEmail)
                .orElseThrow(() -> new RuntimeException("Farmer not found"));

        Orchard orchard = orchardRepository.findById(request.getOrchardId())
                .orElseThrow(() -> new RuntimeException("Orchard not found with id: " + request.getOrchardId()));

        if (!orchard.getFarmerId().equals(farmer.getId())) {
            throw new SecurityException("Access denied: You can only report maturity for your own orchards.");
        }

        Instant now = Instant.now();

        MaturityReport report = new MaturityReport();
        report.setOrchardId(orchard.getId());
        report.setOrchardName(orchard.getName());
        report.setFarmerId(farmer.getId());
        report.setMaturityLevel(request.getMaturityLevel());
        report.setNotes(request.getNotes() != null ? request.getNotes().trim() : null);
        report.setVerified(false);
        report.setReportedAt(now);

        MaturityReport savedReport = maturityReportRepository.save(report);

        // Keep Orchard.currentMaturity in sync for map and summary views
        orchard.setCurrentMaturity(request.getMaturityLevel());
        orchard.setUpdatedAt(now);
        orchardRepository.save(orchard);

        return MaturityReportResponse.fromEntity(savedReport);
    }

    // Farmer consults their own maturity reports
    public List<MaturityReportResponse> getMyReports(String farmerEmail) {
        User farmer = userRepository.findByEmail(farmerEmail)
                .orElseThrow(() -> new RuntimeException("Farmer not found"));

        return maturityReportRepository.findByFarmerIdOrderByReportedAtDesc(farmer.getId())
                .stream()
                .map(MaturityReportResponse::fromEntity)
                .toList();
    }

    // Collection Manager (and Admin) consult submitted maturity reports
    public List<MaturityReportResponse> getReportsForManager(String orchardId, MaturityLevel maturityLevel) {
        List<MaturityReport> reports;

        if (orchardId != null && !orchardId.isBlank()) {
            reports = maturityReportRepository.findByOrchardIdOrderByReportedAtDesc(orchardId);
            if (maturityLevel != null) {
                reports = reports.stream()
                        .filter(r -> r.getMaturityLevel() == maturityLevel)
                        .toList();
            }
        } else if (maturityLevel != null) {
            reports = maturityReportRepository.findByMaturityLevelOrderByReportedAtDesc(maturityLevel);
        } else {
            reports = maturityReportRepository.findAllByOrderByReportedAtDesc();
        }

        return reports.stream()
                .map(MaturityReportResponse::fromEntity)
                .toList();
    }

    // Maturity report history for a specific orchard (owner if FARMER, or authorized staff)
    public List<MaturityReportResponse> getOrchardMaturityHistory(String orchardId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Orchard orchard = orchardRepository.findById(orchardId)
                .orElseThrow(() -> new RuntimeException("Orchard not found with id: " + orchardId));

        if (user.getRole() == Role.FARMER && !orchard.getFarmerId().equals(user.getId())) {
            throw new SecurityException("Access denied: You do not own this orchard.");
        }

        return maturityReportRepository.findByOrchardIdOrderByReportedAtDesc(orchardId)
                .stream()
                .map(MaturityReportResponse::fromEntity)
                .toList();
    }
}