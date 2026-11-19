package ISIMS.olive_connect.repository;

import ISIMS.olive_connect.entity.HarvestRequest;
import ISIMS.olive_connect.entity.HarvestRequestStatus;
import ISIMS.olive_connect.entity.HarvestRequestType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

public interface HarvestRequestRepository extends MongoRepository<HarvestRequest, String> {

    List<HarvestRequest> findByFarmerIdOrderByCreatedAtDesc(String farmerId);

    List<HarvestRequest> findByOrchardIdOrderByCreatedAtDesc(String orchardId);

    List<HarvestRequest> findAllByOrderByCreatedAtDesc();

    List<HarvestRequest> findByStatusOrderByCreatedAtDesc(HarvestRequestStatus status);

    List<HarvestRequest> findByRequestTypeOrderByCreatedAtDesc(HarvestRequestType requestType);

    List<HarvestRequest> findByRequestTypeAndStatusOrderByCreatedAtDesc(
            HarvestRequestType requestType, HarvestRequestStatus status);

    boolean existsByOrchardIdAndStatusIn(String orchardId, Collection<HarvestRequestStatus> statuses);
}