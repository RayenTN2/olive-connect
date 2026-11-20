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
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * US-25: Equipment availability rules, shared by every caller (Logistics Manager now, tour planning in #26/#36/#42).
 * Tour planning must use {@link #getAssignableEquipment} so inactive, under-maintenance or unavailable
 * equipment is never offered for a new tour.
 */
@Service
public class EquipmentAvailabilityService {

    private final EquipmentUnavailabilityRepository unavailabilityRepository;
    private final EquipmentRepository equipmentRepository;
    private final EquipmentService equipmentService;

    public EquipmentAvailabilityService(EquipmentUnavailabilityRepository unavailabilityRepository,
                                        EquipmentRepository equipmentRepository,
                                        EquipmentService equipmentService) {
        this.unavailabilityRepository = unavailabilityRepository;
        this.equipmentRepository = equipmentRepository;
        this.equipmentService = equipmentService;
    }

    // 1. Mark equipment as under maintenance or unavailable for a period (US-25)
    public EquipmentUnavailabilityResponse addUnavailability(String equipmentId, EquipmentUnavailabilityRequest request) {
        equipmentService.findEquipment(equipmentId);
        validatePeriod(request.getStartTime(), request.getEndTime());

        Instant now = Instant.now();
        EquipmentUnavailability unavailability = new EquipmentUnavailability();
        unavailability.setEquipmentId(equipmentId);
        applyRequest(unavailability, request);
        unavailability.setCreatedAt(now);
        unavailability.setUpdatedAt(now);

        return EquipmentUnavailabilityResponse.fromEntity(unavailabilityRepository.save(unavailability));
    }

    // 2. List an item's periods, chronologically (US-25)
    public List<EquipmentUnavailabilityResponse> getUnavailabilities(String equipmentId) {
        equipmentService.findEquipment(equipmentId);

        return unavailabilityRepository.findByEquipmentIdOrderByStartTimeAsc(equipmentId)
                .stream()
                .map(EquipmentUnavailabilityResponse::fromEntity)
                .toList();
    }

    // 3. Edit a period, e.g. extend maintenance or end it early (US-25)
    public EquipmentUnavailabilityResponse updateUnavailability(String equipmentId, String unavailabilityId,
                                                                EquipmentUnavailabilityRequest request) {
        EquipmentUnavailability unavailability = findUnavailability(equipmentId, unavailabilityId);
        validatePeriod(request.getStartTime(), request.getEndTime());

        applyRequest(unavailability, request);
        unavailability.setUpdatedAt(Instant.now());

        return EquipmentUnavailabilityResponse.fromEntity(unavailabilityRepository.save(unavailability));
    }

    // 4. Delete a period, making the equipment available again (US-25).
    // Periods are planning data, not tour history, so a hard delete is fine.
    public void deleteUnavailability(String equipmentId, String unavailabilityId) {
        unavailabilityRepository.delete(findUnavailability(equipmentId, unavailabilityId));
    }

    // 5. Current status, computed from the active flag and the periods covering "now" (US-25)
    public EquipmentStatusResponse getCurrentStatus(String equipmentId) {
        Equipment equipment = equipmentService.findEquipment(equipmentId);
        Instant now = Instant.now();
        List<EquipmentUnavailability> current = unavailabilityRepository
                .findByEquipmentIdAndStartTimeLessThanEqualAndEndTimeGreaterThan(equipmentId, now, now);

        return new EquipmentStatusResponse(equipmentId, computeStatus(equipment.isActive(), current), equipment.isActive());
    }

    // 6. Equipment that can be assigned during [start, end): active AND no overlapping period.
    // Single source of truth for tour planning (#26, #36, #42).
    public List<EquipmentResponse> getAssignableEquipment(Instant start, Instant end) {
        validatePeriod(start, end);
        Set<String> blockedIds = unavailabilityRepository.findByStartTimeLessThanAndEndTimeGreaterThan(end, start)
                .stream()
                .map(EquipmentUnavailability::getEquipmentId)
                .collect(Collectors.toSet());

        return equipmentRepository.findByActive(true)
                .stream()
                .filter(equipment -> !blockedIds.contains(equipment.getId()))
                .map(EquipmentResponse::fromEntity)
                .toList();
    }

    // Priority: UNDER_MAINTENANCE > UNAVAILABLE (period or inactive) > AVAILABLE. IN_USE comes with tours (#35/#36).
    static EquipmentStatus computeStatus(boolean active, List<EquipmentUnavailability> currentPeriods) {
        boolean underMaintenance = currentPeriods.stream()
                .anyMatch(period -> period.getStatus() == EquipmentUnavailabilityStatus.UNDER_MAINTENANCE);
        if (underMaintenance) {
            return EquipmentStatus.UNDER_MAINTENANCE;
        }
        if (!active || !currentPeriods.isEmpty()) {
            return EquipmentStatus.UNAVAILABLE;
        }
        return EquipmentStatus.AVAILABLE;
    }

    private void applyRequest(EquipmentUnavailability unavailability, EquipmentUnavailabilityRequest request) {
        unavailability.setStartTime(request.getStartTime());
        unavailability.setEndTime(request.getEndTime());
        unavailability.setStatus(request.getStatus());
        unavailability.setReason(request.getReason());
    }

    private EquipmentUnavailability findUnavailability(String equipmentId, String unavailabilityId) {
        equipmentService.findEquipment(equipmentId);
        return unavailabilityRepository.findByIdAndEquipmentId(unavailabilityId, equipmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Unavailability not found with id: " + unavailabilityId + " for equipment: " + equipmentId));
    }

    private void validatePeriod(Instant start, Instant end) {
        if (start == null || end == null) {
            throw new RuntimeException("Start time and end time are required.");
        }
        if (!start.isBefore(end)) {
            throw new RuntimeException("Start time must be before end time.");
        }
    }
}
