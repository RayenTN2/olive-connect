package ISIMS.olive_connect.repository;

import ISIMS.olive_connect.entity.CollectionTour;
import ISIMS.olive_connect.entity.CollectionTourStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

public interface CollectionTourRepository extends MongoRepository<CollectionTour, String> {
    List<CollectionTour> findByStatusOrderByPlannedStartTimeAsc(CollectionTourStatus status);
    List<CollectionTour> findByStatusIn(Collection<CollectionTourStatus> statuses);
    List<CollectionTour> findAllByOrderByCreatedAtDesc();
}