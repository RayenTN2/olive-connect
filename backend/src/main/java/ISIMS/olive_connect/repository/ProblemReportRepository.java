package ISIMS.olive_connect.repository;

import ISIMS.olive_connect.entity.ProblemReport;
import ISIMS.olive_connect.entity.ProblemStatus;
import ISIMS.olive_connect.entity.ProblemType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProblemReportRepository extends MongoRepository<ProblemReport, String> {

    List<ProblemReport> findByOrchardIdOrderByReportedAtDesc(String orchardId);

    List<ProblemReport> findByFarmerIdOrderByReportedAtDesc(String farmerId);

    List<ProblemReport> findAllByOrderByReportedAtDesc();

    List<ProblemReport> findByStatusOrderByReportedAtDesc(ProblemStatus status);

    List<ProblemReport> findByProblemTypeOrderByReportedAtDesc(ProblemType problemType);

    List<ProblemReport> findByProblemTypeAndStatusOrderByReportedAtDesc(ProblemType problemType, ProblemStatus status);
}