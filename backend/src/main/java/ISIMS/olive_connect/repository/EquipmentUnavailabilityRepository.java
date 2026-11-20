package ISIMS.olive_connect.repository;

import ISIMS.olive_connect.entity.EquipmentUnavailability;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface EquipmentUnavailabilityRepository extends MongoRepository<EquipmentUnavailability, String> {

    // US-25: Periods of one equipment item, chronologically
    List<EquipmentUnavailability> findByEquipmentIdOrderByStartTimeAsc(String equipmentId);

    // US-25: Ensures a period is only edited/deleted through its own equipment
    Optional<EquipmentUnavailability> findByIdAndEquipmentId(String id, String equipmentId);

    // US-25: Periods of one item covering instant t — start <= t < end.
    // Call as findByEquipmentIdAndStartTimeLessThanEqualAndEndTimeGreaterThan(id, t, t).
    List<EquipmentUnavailability> findByEquipmentIdAndStartTimeLessThanEqualAndEndTimeGreaterThan(
            String equipmentId, Instant at, Instant atAgain);

    // US-25: Overlap rule (same as US-23) — period.start < rangeEnd AND period.end > rangeStart.
    // Call as findByStartTimeLessThanAndEndTimeGreaterThan(rangeEnd, rangeStart).
    List<EquipmentUnavailability> findByStartTimeLessThanAndEndTimeGreaterThan(Instant rangeEnd, Instant rangeStart);
}
