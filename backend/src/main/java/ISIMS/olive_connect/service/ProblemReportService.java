package ISIMS.olive_connect.service;

import ISIMS.olive_connect.dto.ProblemReportResponse;
import ISIMS.olive_connect.dto.ReportProblemRequest;
import ISIMS.olive_connect.entity.Orchard;
import ISIMS.olive_connect.entity.ProblemReport;
import ISIMS.olive_connect.entity.ProblemStatus;
import ISIMS.olive_connect.entity.ProblemType;
import ISIMS.olive_connect.entity.Role;
import ISIMS.olive_connect.entity.User;
import ISIMS.olive_connect.repository.OrchardRepository;
import ISIMS.olive_connect.repository.ProblemReportRepository;
import ISIMS.olive_connect.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProblemReportService {

    private final ProblemReportRepository problemReportRepository;
    private final OrchardRepository orchardRepository;
    private final UserRepository userRepository;

    public ProblemReportService(ProblemReportRepository problemReportRepository,
                                OrchardRepository orchardRepository,
                                UserRepository userRepository) {
        this.problemReportRepository = problemReportRepository;
        this.orchardRepository = orchardRepository;
        this.userRepository = userRepository;
    }

    public ProblemReportResponse reportProblem(ReportProblemRequest request, String farmerEmail) {
        User farmer = userRepository.findByEmail(farmerEmail)
                .orElseThrow(() -> new RuntimeException("Farmer not found"));

        Orchard orchard = orchardRepository.findById(request.getOrchardId())
                .orElseThrow(() -> new RuntimeException("Orchard not found with id: " + request.getOrchardId()));

        if (!orchard.getFarmerId().equals(farmer.getId())) {
            throw new SecurityException("Access denied: You can only report problems for your own orchards.");
        }

        ProblemReport report = new ProblemReport();
        report.setOrchardId(orchard.getId());
        report.setFarmerId(farmer.getId());
        report.setProblemType(request.getProblemType());
        report.setDescription(request.getDescription().trim());
        report.setStatus(ProblemStatus.PENDING_REVIEW);
        report.setReportedAt(Instant.now());

        ProblemReport savedReport = problemReportRepository.save(report);
        return ProblemReportResponse.fromEntity(savedReport, orchard);
    }

    public List<ProblemReportResponse> getMyReports(String farmerEmail) {
        User farmer = userRepository.findByEmail(farmerEmail)
                .orElseThrow(() -> new RuntimeException("Farmer not found"));

        List<ProblemReport> reports = problemReportRepository.findByFarmerIdOrderByReportedAtDesc(farmer.getId());
        return mapWithOrchards(reports);
    }

    public List<ProblemReportResponse> getReportsForManager(ProblemType problemType, ProblemStatus status, String orchardId) {
        List<ProblemReport> reports;

        if (orchardId != null && !orchardId.isBlank()) {
            reports = problemReportRepository.findByOrchardIdOrderByReportedAtDesc(orchardId)
                    .stream()
                    .filter(r -> problemType == null || r.getProblemType() == problemType)
                    .filter(r -> status == null || r.getStatus() == status)
                    .toList();
        } else if (problemType != null && status != null) {
            reports = problemReportRepository.findByProblemTypeAndStatusOrderByReportedAtDesc(problemType, status);
        } else if (problemType != null) {
            reports = problemReportRepository.findByProblemTypeOrderByReportedAtDesc(problemType);
        } else if (status != null) {
            reports = problemReportRepository.findByStatusOrderByReportedAtDesc(status);
        } else {
            reports = problemReportRepository.findAllByOrderByReportedAtDesc();
        }

        return mapWithOrchards(reports);
    }

    public ProblemReportResponse getReportById(String reportId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ProblemReport report = problemReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Problem report not found with id: " + reportId));

        if (user.getRole() == Role.FARMER && !report.getFarmerId().equals(user.getId())) {
            throw new SecurityException("Access denied: You do not own this problem report.");
        }

        Orchard orchard = orchardRepository.findById(report.getOrchardId()).orElse(null);
        return ProblemReportResponse.fromEntity(report, orchard);
    }

    public ProblemReportResponse updateReportStatus(String reportId, ProblemStatus newStatus) {
        if (newStatus == ProblemStatus.PENDING_REVIEW) {
            throw new IllegalArgumentException("Status cannot be reset to PENDING_REVIEW once processing has started.");
        }

        ProblemReport report = problemReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Problem report not found with id: " + reportId));

        report.setStatus(newStatus);
        ProblemReport updatedReport = problemReportRepository.save(report);
        Orchard orchard = orchardRepository.findById(updatedReport.getOrchardId()).orElse(null);
        return ProblemReportResponse.fromEntity(updatedReport, orchard);
    }

    private List<ProblemReportResponse> mapWithOrchards(List<ProblemReport> reports) {
        Set<String> orchardIds = reports.stream()
                .map(ProblemReport::getOrchardId)
                .collect(Collectors.toSet());

        Map<String, Orchard> orchardMap = orchardRepository.findAllById(orchardIds)
                .stream()
                .collect(Collectors.toMap(Orchard::getId, Function.identity()));

        return reports.stream()
                .map(report -> ProblemReportResponse.fromEntity(report, orchardMap.get(report.getOrchardId())))
                .toList();
    }
}