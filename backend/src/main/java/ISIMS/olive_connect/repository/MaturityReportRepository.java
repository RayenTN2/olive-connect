package ISIMS.olive_connect.repository;

import ISIMS.olive_connect.entity.MaturityLevel;
import ISIMS.olive_connect.entity.MaturityReport;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MaturityReportRepository extends MongoRepository<MaturityReport, String> {

    List<MaturityReport> findByOrchardIdOrderByReportedAtDesc(String orchardId);

    List<MaturityReport> findByFarmerIdOrderByReportedAtDesc(String farmerId);

    List<MaturityReport> findAllByOrderByReportedAtDesc();

    List<MaturityReport> findByMaturityLevelOrderByReportedAtDesc(MaturityLevel maturityLevel);
}