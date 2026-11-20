package ISIMS.olive_connect;

import ISIMS.olive_connect.dto.EquipmentResponse;
import ISIMS.olive_connect.entity.Equipment;
import ISIMS.olive_connect.entity.EquipmentStatus;
import ISIMS.olive_connect.entity.EquipmentUnavailability;
import ISIMS.olive_connect.entity.EquipmentUnavailabilityStatus;
import ISIMS.olive_connect.repository.EquipmentRepository;
import ISIMS.olive_connect.repository.EquipmentUnavailabilityRepository;
import ISIMS.olive_connect.service.EquipmentAvailabilityService;
import ISIMS.olive_connect.service.EquipmentService;
import ISIMS.olive_connect.service.EquipmentTypeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

import static ISIMS.olive_connect.entity.EquipmentUnavailabilityStatus.UNAVAILABLE;
import static ISIMS.olive_connect.entity.EquipmentUnavailabilityStatus.UNDER_MAINTENANCE;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * US-25: Runs the status and assignability rules against a real, throw-away MongoDB (Docker).
 * Never touches Atlas. Skipped automatically when Docker is not running.
 */
@DataMongoTest
@Testcontainers(disabledWithoutDocker = true)
@Import({EquipmentAvailabilityService.class, EquipmentService.class, EquipmentTypeService.class})
class EquipmentAvailabilityIntegrationTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    // Proposed tour period: 10:00 → 12:00
    private static final Instant TOUR_START = at("10:00");
    private static final Instant TOUR_END = at("12:00");

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private EquipmentUnavailabilityRepository unavailabilityRepository;

    @Autowired
    private EquipmentAvailabilityService availabilityService;

    private static Instant at(String time) {
        return Instant.parse("2026-11-25T" + time + ":00Z");
    }

    @BeforeEach
    void cleanDatabase() {
        unavailabilityRepository.deleteAll();
        equipmentRepository.deleteAll();
    }

    private String saveEquipment(String reference, boolean active) {
        Equipment equipment = new Equipment();
        equipment.setName(reference);
        equipment.setReference(reference);
        equipment.setType("NET");
        equipment.setActive(active);
        return equipmentRepository.save(equipment).getId();
    }

    private void savePeriod(String equipmentId, Instant start, Instant end, EquipmentUnavailabilityStatus status) {
        EquipmentUnavailability period = new EquipmentUnavailability();
        period.setEquipmentId(equipmentId);
        period.setStartTime(start);
        period.setEndTime(end);
        period.setStatus(status);
        period.setReason("test");
        unavailabilityRepository.save(period);
    }

    @Test
    void assignableEquipment_excludesInactiveAndOverlappingPeriods_butNotTouchingOnes() {
        String free = saveEquipment("FREE", true);
        String touching = saveEquipment("TOUCHING", true);       // maintenance ends exactly when the tour starts
        String maintenance = saveEquipment("MAINTENANCE", true);
        String unavailable = saveEquipment("UNAVAILABLE", true);
        saveEquipment("INACTIVE", false);
        savePeriod(touching, at("08:00"), at("10:00"), UNDER_MAINTENANCE);
        savePeriod(maintenance, at("11:00"), at("13:00"), UNDER_MAINTENANCE);
        savePeriod(unavailable, at("09:00"), at("10:30"), UNAVAILABLE);

        Set<String> assignable = availabilityService.getAssignableEquipment(TOUR_START, TOUR_END)
                .stream()
                .map(EquipmentResponse::getId)
                .collect(Collectors.toSet());

        assertEquals(Set.of(free, touching), assignable);
    }

    @Test
    void currentStatus_reflectsPeriodsCoveringNow() {
        Instant now = Instant.now();
        Duration hour = Duration.ofHours(1);
        String available = saveEquipment("AVAILABLE", true);
        String pastOnly = saveEquipment("PAST", true);
        String unavailable = saveEquipment("UNAVAILABLE", true);
        String both = saveEquipment("BOTH", true);
        String inactive = saveEquipment("INACTIVE", false);
        savePeriod(pastOnly, now.minus(hour.multipliedBy(3)), now.minus(hour), UNDER_MAINTENANCE);
        savePeriod(unavailable, now.minus(hour), now.plus(hour), UNAVAILABLE);
        savePeriod(both, now.minus(hour), now.plus(hour), UNAVAILABLE);
        savePeriod(both, now.minus(hour), now.plus(hour), UNDER_MAINTENANCE);

        assertEquals(EquipmentStatus.AVAILABLE, availabilityService.getCurrentStatus(available).getStatus());
        assertEquals(EquipmentStatus.AVAILABLE, availabilityService.getCurrentStatus(pastOnly).getStatus());
        assertEquals(EquipmentStatus.UNAVAILABLE, availabilityService.getCurrentStatus(unavailable).getStatus());
        assertEquals(EquipmentStatus.UNDER_MAINTENANCE, availabilityService.getCurrentStatus(both).getStatus());
        assertEquals(EquipmentStatus.UNAVAILABLE, availabilityService.getCurrentStatus(inactive).getStatus());
    }
}
