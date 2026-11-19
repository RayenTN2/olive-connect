package ISIMS.olive_connect.repository;

import ISIMS.olive_connect.entity.WorkerUnavailability;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface WorkerUnavailabilityRepository extends MongoRepository<WorkerUnavailability, String> {

    // US-23: Intervals of one worker, chronologically
    List<WorkerUnavailability> findByWorkerIdOrderByStartTimeAsc(String workerId);

    // US-23: Ensures an interval is only edited/deleted through its own worker
    Optional<WorkerUnavailability> findByIdAndWorkerId(String id, String workerId);

    // US-23: Overlap rule — interval.start < periodEnd AND interval.end > periodStart.
    // Call as findByStartTimeLessThanAndEndTimeGreaterThan(periodEnd, periodStart).
    List<WorkerUnavailability> findByStartTimeLessThanAndEndTimeGreaterThan(Instant periodEnd, Instant periodStart);
}
