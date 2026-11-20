package ISIMS.olive_connect.service;

import ISIMS.olive_connect.dto.CreateEquipmentTypeRequest;
import ISIMS.olive_connect.dto.EquipmentTypeResponse;
import ISIMS.olive_connect.entity.CustomEquipmentType;
import ISIMS.olive_connect.entity.EquipmentType;
import ISIMS.olive_connect.repository.CustomEquipmentTypeRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Service
public class EquipmentTypeService {

    private final CustomEquipmentTypeRepository customTypeRepository;

    public EquipmentTypeService(CustomEquipmentTypeRepository customTypeRepository) {
        this.customTypeRepository = customTypeRepository;
    }

    // 1. List built-in types first, then custom ones (US-24)
    public List<EquipmentTypeResponse> getTypes() {
        return Stream.concat(
                Arrays.stream(EquipmentType.values()).map(EquipmentTypeResponse::fromBuiltIn),
                customTypeRepository.findAll().stream().map(EquipmentTypeResponse::fromCustom)
        ).toList();
    }

    // 2. Add a custom type; "Trailer bin" → code TRAILER_BIN (US-24)
    public EquipmentTypeResponse createType(CreateEquipmentTypeRequest request) {
        String label = request.getLabel().trim();
        String code = toCode(label);
        if (code.isEmpty()) {
            throw new RuntimeException("Label must contain letters or digits.");
        }
        if (isKnownType(code)) {
            throw new RuntimeException("Equipment type already exists: " + code);
        }

        CustomEquipmentType type = new CustomEquipmentType();
        type.setCode(code);
        type.setLabel(label);
        type.setCreatedAt(Instant.now());

        return EquipmentTypeResponse.fromCustom(customTypeRepository.save(type));
    }

    // Used by EquipmentService: an equipment's type must be a built-in or custom code
    public void requireKnownType(String code) {
        if (!isKnownType(code)) {
            throw new RuntimeException("Unknown equipment type: " + code);
        }
    }

    private boolean isKnownType(String code) {
        return EquipmentType.isBuiltIn(code) || customTypeRepository.existsByCode(code);
    }

    // Upper-case, and turn every run of non letters/digits into "_" (accents and Arabic letters are kept)
    static String toCode(String label) {
        return label.trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", "_")
                .replaceAll("^_+|_+$", "");
    }
}
