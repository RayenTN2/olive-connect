package ISIMS.olive_connect;

import ISIMS.olive_connect.dto.WorkerResponse;
import ISIMS.olive_connect.entity.Worker;
import ISIMS.olive_connect.entity.WorkerUnavailability;
import ISIMS.olive_connect.repository.WorkerRepository;
import ISIMS.olive_connect.repository.WorkerUnavailabilityRepository;
import ISIMS.olive_connect.service.WorkerAvailabilityService;
import ISIMS.olive_connect.service.WorkerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * US-23: Runs the overlap query and availability rules against a real, throw-away MongoDB (Docker).
 * Never touches Atlas. Skipped automatically when Docker is not running.
 */
@DataMongoTest
@Testcontainers(disabledWithoutDocker = true)
@Import({WorkerAvailabilityService.class, WorkerService.class})
class WorkerAvailabilityIntegrationTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    // Proposed tour period: 10:00 → 12:00
    private static final Instant TOUR_START = at("10:00");
    private static final Instant TOUR_END = at("12:00");

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private WorkerUnavailabilityRepository unavailabilityRepository;

    @Autowired
    private WorkerAvailabilityService availabilityService;

    private static Instant at(String time) {
        return Instant.parse("2026-11-20T" + time + ":00Z");
    }

    @BeforeEach
    void cleanDatabase() {
        unavailabilityRepository.deleteAll();
        workerRepository.deleteAll();
    }

    private Worker saveWorker(String name, boolean active) {
        Worker worker = new Worker();
        worker.setFullName(name);
        worker.setPhone("+21600000000");
        worker.setActive(active);
        return workerRepository.save(worker);
    }

    private void saveInterval(String workerId, String start, String end, String reason) {
        WorkerUnavailability interval = new WorkerUnavailability();
        interval.setWorkerId(workerId);
        interval.setStartTime(at(start));
        interval.setEndTime(at(end));
        interval.setReason(reason);
        unavailabilityRepository.save(interval);
    }

    @Test
    void overlapQuery_matchesOnlyTrulyOverlappingIntervals() {
        String w = saveWorker("Ali", true).getId();
        saveInterval(w, "10:30", "11:00", "inside");
        saveInterval(w, "09:00", "10:30", "overlapsStart");
        saveInterval(w, "11:30", "13:00", "overlapsEnd");
        saveInterval(w, "08:00", "14:00", "coversTour");
        saveInterval(w, "08:00", "10:00", "endsWhenTourStarts");
        saveInterval(w, "12:00", "13:00", "startsWhenTourEnds");
        saveInterval(w, "06:00", "07:00", "before");

        Set<String> matched = unavailabilityRepository
                .findByStartTimeLessThanAndEndTimeGreaterThan(TOUR_END, TOUR_START)
                .stream()
                .map(WorkerUnavailability::getReason)
                .collect(Collectors.toSet());

        assertEquals(Set.of("inside", "overlapsStart", "overlapsEnd", "coversTour"), matched);
    }

    @Test
    void unavailableWorkers_listsEachWorkerOnce() {
        String busy = saveWorker("Busy", true).getId();
        String free = saveWorker("Free", true).getId();
        saveInterval(busy, "09:00", "10:30", "Congé");
        saveInterval(busy, "11:00", "13:00", "Absence");
        saveInterval(free, "13:00", "15:00", "Congé");

        List<WorkerResponse> unavailable = availabilityService.getUnavailableWorkers(TOUR_START, TOUR_END);

        assertEquals(List.of(busy), unavailable.stream().map(WorkerResponse::getId).toList());
    }

    @Test
    void assignableWorkers_excludeInactiveAndUnavailableWorkers() {
        String activeFree = saveWorker("Active free", true).getId();
        String activeTouching = saveWorker("Active, absent until tour start", true).getId();
        String activeBusy = saveWorker("Active busy", true).getId();
        saveWorker("Inactive free", false);
        saveInterval(activeTouching, "08:00", "10:00", "Absence");
        saveInterval(activeBusy, "11:00", "11:30", "Congé");

        Set<String> assignable = availabilityService.getAssignableWorkers(TOUR_START, TOUR_END)
                .stream()
                .map(WorkerResponse::getId)
                .collect(Collectors.toSet());

        assertEquals(Set.of(activeFree, activeTouching), assignable);
    }
}
