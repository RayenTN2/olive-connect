package ISIMS.olive_connect.service;

import ISIMS.olive_connect.dto.EquipmentRequest;
import ISIMS.olive_connect.dto.EquipmentResponse;
import ISIMS.olive_connect.entity.Equipment;
import ISIMS.olive_connect.entity.EquipmentType;
import ISIMS.olive_connect.exception.ResourceNotFoundException;
import ISIMS.olive_connect.repository.EquipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;

@Service
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final EquipmentTypeService equipmentTypeService;

    public EquipmentService(EquipmentRepository equipmentRepository, EquipmentTypeService equipmentTypeService) {
        this.equipmentRepository = equipmentRepository;
        this.equipmentTypeService = equipmentTypeService;
    }

    // 1. Register new equipment, active by default (US-24)
    public EquipmentResponse createEquipment(EquipmentRequest request) {
        validateTypeAndSpecs(request);
        if (equipmentRepository.existsByReference(request.getReference())) {
            throw new RuntimeException("Reference is already in use: " + request.getReference());
        }

        Instant now = Instant.now();
        Equipment equipment = new Equipment();
        applyRequest(equipment, request);
        equipment.setActive(true);
        equipment.setCreatedAt(now);
        equipment.setUpdatedAt(now);

        return EquipmentResponse.fromEntity(equipmentRepository.save(equipment));
    }

    // 2. List equipment, optionally filtered by type and/or active status (US-24)
    public List<EquipmentResponse> getEquipment(String type, Boolean active) {
        if (type != null) {
            equipmentTypeService.requireKnownType(type);
        }

        List<Equipment> equipment;
        if (type != null && active != null) {
            equipment = equipmentRepository.findByTypeAndActive(type, active);
        } else if (type != null) {
            equipment = equipmentRepository.findByType(type);
        } else if (active != null) {
            equipment = equipmentRepository.findByActive(active);
        } else {
            equipment = equipmentRepository.findAll();
        }

        return equipment.stream()
                .map(EquipmentResponse::fromEntity)
                .toList();
    }

    // 3. Consult one equipment item (US-24)
    public EquipmentResponse getEquipmentById(String equipmentId) {
        return EquipmentResponse.fromEntity(findEquipment(equipmentId));
    }

    // 4. Update equipment details; all fields are replaced (US-24)
    public EquipmentResponse updateEquipment(String equipmentId, EquipmentRequest request) {
        Equipment equipment = findEquipment(equipmentId);
        validateTypeAndSpecs(request);
        if (equipmentRepository.existsByReferenceAndIdNot(request.getReference(), equipmentId)) {
            throw new RuntimeException("Reference is already in use: " + request.getReference());
        }

        applyRequest(equipment, request);
        equipment.setUpdatedAt(Instant.now());

        return EquipmentResponse.fromEntity(equipmentRepository.save(equipment));
    }

    // 5. Activate or deactivate equipment (US-24).
    // Deactivation never deletes the record, so past tours keep a valid equipment reference.
    public EquipmentResponse updateEquipmentStatus(String equipmentId, boolean active) {
        Equipment equipment = findEquipment(equipmentId);
        equipment.setActive(active);
        equipment.setUpdatedAt(Instant.now());

        return EquipmentResponse.fromEntity(equipmentRepository.save(equipment));
    }

    // Public so later stories (#25, #26, #36) can reuse it: unknown id → 404
    public Equipment findEquipment(String equipmentId) {
        return equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found with id: " + equipmentId));
    }

    // Type must exist; capacityKg is BIN-only and required; registrationNumber is TRACTOR-only (blank = not given)
    private void validateTypeAndSpecs(EquipmentRequest request) {
        equipmentTypeService.requireKnownType(request.getType());
        boolean isBin = EquipmentType.BIN.name().equals(request.getType());
        boolean isTractor = EquipmentType.TRACTOR.name().equals(request.getType());

        if (isBin && request.getCapacityKg() == null) {
            throw new RuntimeException("Capacity is required for bins.");
        }
        if (!isBin && request.getCapacityKg() != null) {
            throw new RuntimeException("Capacity is only allowed for bins.");
        }
        if (!isTractor && StringUtils.hasText(request.getRegistrationNumber())) {
            throw new RuntimeException("Registration number is only allowed for tractors.");
        }
    }

    // Forms send empty fields as "": store them as missing
    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

    private void applyRequest(Equipment equipment, EquipmentRequest request) {
        equipment.setName(request.getName());
        equipment.setReference(request.getReference());
        equipment.setType(request.getType());
        equipment.setBrand(blankToNull(request.getBrand()));
        equipment.setModel(blankToNull(request.getModel()));
        equipment.setSerialNumber(blankToNull(request.getSerialNumber()));
        equipment.setRegistrationNumber(blankToNull(request.getRegistrationNumber()));
        equipment.setCapacityKg(request.getCapacityKg());
    }
}
