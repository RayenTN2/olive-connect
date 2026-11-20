package ISIMS.olive_connect.service;

import ISIMS.olive_connect.dto.EquipmentResponse;
import ISIMS.olive_connect.dto.EquipmentStatusResponse;
import ISIMS.olive_connect.dto.EquipmentUnavailabilityRequest;
import ISIMS.olive_connect.dto.EquipmentUnavailabilityResponse;
import ISIMS.olive_connect.entity.Equipment;
import ISIMS.olive_connect.entity.EquipmentStatus;
import ISIMS.olive_connect.entity.EquipmentUnavailability;
import ISIMS.olive_connect.entity.EquipmentUnavailabilityStatus;
import ISIMS.olive_connect.exception.ResourceNotFoundException;
import ISIMS.olive_connect.repository.EquipmentRepository;
import ISIMS.olive_connect.repository.EquipmentUnavailabilityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static ISIMS.olive_connect.entity.EquipmentUnavailabilityStatus.UNAVAILABLE;
import static ISIMS.olive_connect.entity.EquipmentUnavailabilityStatus.UNDER_MAINTENANCE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EquipmentAvailabilityServiceTest {

    private static final Instant T10 = Instant.parse("2026-11-25T10:00:00Z");
    private static final Instant T12 = Instant.parse("2026-11-25T12:00:00Z");
    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private EquipmentUnavailabilityRepository unavailabilityRepository;

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private EquipmentService equipmentService;

    @InjectMocks
    private EquipmentAvailabilityService service;

    @BeforeEach
    void setUp() {
        lenient().when(unavailabilityRepository.save(any(EquipmentUnavailability.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private static EquipmentUnavailabilityRequest request(Instant start, Instant end, EquipmentUnavailabilityStatus status) {
        EquipmentUnavailabilityRequest request = new EquipmentUnavailabilityRequest();
        request.setStartTime(start);
        request.setEndTime(end);
        request.setStatus(status);
        request.setReason("Oil change");
        return request;
    }

    private static Equipment equipment(String id, boolean active) {
        Equipment equipment = new Equipment();
        equipment.setId(id);
        equipment.setActive(active);
        return equipment;
    }

    private static EquipmentUnavailability period(String id, String equipmentId, EquipmentUnavailabilityStatus status) {
        EquipmentUnavailability period = new EquipmentUnavailability();
        period.setId(id);
        period.setEquipmentId(equipmentId);
        period.setStartTime(T10);
        period.setEndTime(T12);
        period.setStatus(status);
        period.setReason("Breakdown");
        period.setCreatedAt(CREATED);
        period.setUpdatedAt(CREATED);
        return period;
    }

    // --- create ---

    @Test
    void addUnavailability_savesPeriodForEquipment() {
        EquipmentUnavailabilityResponse response = service.addUnavailability("e1", request(T10, T12, UNDER_MAINTENANCE));

        verify(equipmentService).findEquipment("e1");
        assertEquals("e1", response.getEquipmentId());
        assertEquals(T10, response.getStartTime());
        assertEquals(T12, response.getEndTime());
        assertEquals(UNDER_MAINTENANCE, response.getStatus());
        assertEquals("Oil change", response.getReason());
        assertNotNull(response.getCreatedAt());
    }

    @Test
    void addUnavailability_startNotBeforeEnd_isRejected() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.addUnavailability("e1", request(T10, T10, UNAVAILABLE)));
        assertEquals("Start time must be before end time.", ex.getMessage());
        assertThrows(RuntimeException.class, () -> service.addUnavailability("e1", request(T12, T10, UNAVAILABLE)));
        verify(unavailabilityRepository, never()).save(any());
    }

    @Test
    void addUnavailability_unknownEquipment_isNotFound() {
        when(equipmentService.findEquipment("missing"))
                .thenThrow(new ResourceNotFoundException("Equipment not found with id: missing"));

        assertThrows(ResourceNotFoundException.class,
                () -> service.addUnavailability("missing", request(T10, T12, UNAVAILABLE)));
        verify(unavailabilityRepository, never()).save(any());
    }

    // --- list / edit / delete ---

    @Test
    void getUnavailabilities_returnsEquipmentPeriods() {
        when(unavailabilityRepository.findByEquipmentIdOrderByStartTimeAsc("e1"))
                .thenReturn(List.of(period("u1", "e1", UNAVAILABLE), period("u2", "e1", UNDER_MAINTENANCE)));

        assertEquals(2, service.getUnavailabilities("e1").size());
    }

    @Test
    void updateUnavailability_changesFieldsAndKeepsCreatedAt() {
        when(unavailabilityRepository.findByIdAndEquipmentId("u1", "e1"))
                .thenReturn(Optional.of(period("u1", "e1", UNAVAILABLE)));
        Instant newEnd = Instant.parse("2026-11-26T12:00:00Z");

        EquipmentUnavailabilityResponse response =
                service.updateUnavailability("e1", "u1", request(T10, newEnd, UNDER_MAINTENANCE));

        assertEquals(newEnd, response.getEndTime());
        assertEquals(UNDER_MAINTENANCE, response.getStatus());
        assertEquals(CREATED, response.getCreatedAt());
        assertTrue(response.getUpdatedAt().isAfter(CREATED));
    }

    @Test
    void updateUnavailability_periodOfAnotherEquipment_isNotFound() {
        when(unavailabilityRepository.findByIdAndEquipmentId("u1", "e2")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateUnavailability("e2", "u1", request(T10, T12, UNAVAILABLE)));
        verify(unavailabilityRepository, never()).save(any());
    }

    @Test
    void updateUnavailability_invalidPeriod_isRejected() {
        when(unavailabilityRepository.findByIdAndEquipmentId("u1", "e1"))
                .thenReturn(Optional.of(period("u1", "e1", UNAVAILABLE)));

        assertThrows(RuntimeException.class,
                () -> service.updateUnavailability("e1", "u1", request(T12, T10, UNAVAILABLE)));
        verify(unavailabilityRepository, never()).save(any());
    }

    @Test
    void deleteUnavailability_deletesOnlyThePeriod() {
        EquipmentUnavailability period = period("u1", "e1", UNAVAILABLE);
        when(unavailabilityRepository.findByIdAndEquipmentId("u1", "e1")).thenReturn(Optional.of(period));

        service.deleteUnavailability("e1", "u1");

        verify(unavailabilityRepository).delete(period);
        verifyNoInteractions(equipmentRepository); // equipment itself is never deleted
    }

    @Test
    void deleteUnavailability_unknownPeriod_isNotFound() {
        when(unavailabilityRepository.findByIdAndEquipmentId("missing", "e1")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteUnavailability("e1", "missing"));
        verify(unavailabilityRepository, never()).delete(any());
    }

    // --- status ---

    @Test
    void computeStatus_followsPriority() {
        List<EquipmentUnavailability> none = List.of();
        List<EquipmentUnavailability> unavailable = List.of(period("u1", "e1", UNAVAILABLE));
        List<EquipmentUnavailability> both = List.of(period("u1", "e1", UNAVAILABLE), period("u2", "e1", UNDER_MAINTENANCE));

        assertEquals(EquipmentStatus.AVAILABLE, EquipmentAvailabilityService.computeStatus(true, none));
        assertEquals(EquipmentStatus.UNAVAILABLE, EquipmentAvailabilityService.computeStatus(true, unavailable));
        assertEquals(EquipmentStatus.UNAVAILABLE, EquipmentAvailabilityService.computeStatus(false, none));
        assertEquals(EquipmentStatus.UNDER_MAINTENANCE, EquipmentAvailabilityService.computeStatus(true, both));
        assertEquals(EquipmentStatus.UNDER_MAINTENANCE,
                EquipmentAvailabilityService.computeStatus(false, List.of(period("u2", "e1", UNDER_MAINTENANCE))));
    }

    @Test
    void getCurrentStatus_usesPeriodsCoveringNow() {
        when(equipmentService.findEquipment("e1")).thenReturn(equipment("e1", true));
        when(unavailabilityRepository.findByEquipmentIdAndStartTimeLessThanEqualAndEndTimeGreaterThan(eq("e1"), any(), any()))
                .thenReturn(List.of(period("u1", "e1", UNDER_MAINTENANCE)));

        EquipmentStatusResponse response = service.getCurrentStatus("e1");

        assertEquals("e1", response.getEquipmentId());
        assertEquals(EquipmentStatus.UNDER_MAINTENANCE, response.getStatus());
        assertTrue(response.isActive());
    }

    @Test
    void getCurrentStatus_unknownEquipment_isNotFound() {
        when(equipmentService.findEquipment("missing"))
                .thenThrow(new ResourceNotFoundException("Equipment not found with id: missing"));

        assertThrows(ResourceNotFoundException.class, () -> service.getCurrentStatus("missing"));
    }

    // --- assignable equipment ---

    @Test
    void getAssignableEquipment_returnsOnlyActiveEquipmentWithoutOverlap() {
        when(unavailabilityRepository.findByStartTimeLessThanAndEndTimeGreaterThan(T12, T10))
                .thenReturn(List.of(period("u1", "busy", UNDER_MAINTENANCE)));
        when(equipmentRepository.findByActive(true)).thenReturn(List.of(equipment("free", true), equipment("busy", true)));

        List<EquipmentResponse> result = service.getAssignableEquipment(T10, T12);

        assertEquals(List.of("free"), result.stream().map(EquipmentResponse::getId).toList());
        verify(equipmentRepository, never()).findAll(); // inactive equipment is never loaded
    }

    @Test
    void getAssignableEquipment_invalidPeriod_isRejected() {
        assertThrows(RuntimeException.class, () -> service.getAssignableEquipment(T12, T10));
        verifyNoInteractions(unavailabilityRepository);
    }
}
