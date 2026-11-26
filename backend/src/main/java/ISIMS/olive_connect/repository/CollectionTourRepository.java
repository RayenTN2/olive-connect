package ISIMS.olive_connect.repository;

import ISIMS.olive_connect.entity.CollectionTour;
import ISIMS.olive_connect.entity.CollectionTourStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Collection;
import java.util.List;

public interface CollectionTourRepository extends MongoRepository<CollectionTour, String> {

    // Lists leave out the route (about 900 points per tour): they don't draw it.
    // Never save a tour read this way, its route would be erased: load it with findById.
    @Query(fields = "{ 'routeGeometry': 0 }")
    List<CollectionTour> findByStatusOrderByPlannedStartTimeAsc(CollectionTourStatus status);

    @Query(fields = "{ 'routeGeometry': 0 }")
    List<CollectionTour> findByStatusIn(Collection<CollectionTourStatus> statuses);

    @Query(fields = "{ 'routeGeometry': 0 }")
    List<CollectionTour> findAllByOrderByCreatedAtDesc();
}