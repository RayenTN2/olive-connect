package ISIMS.olive_connect.service;

import ISIMS.olive_connect.dto.OilMillRequest;
import ISIMS.olive_connect.dto.OilMillResponse;
import ISIMS.olive_connect.entity.OilMill;
import ISIMS.olive_connect.exception.ResourceNotFoundException;
import ISIMS.olive_connect.repository.OilMillRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class OilMillService {

    private final OilMillRepository oilMillRepository;

    public OilMillService(OilMillRepository oilMillRepository) {
        this.oilMillRepository = oilMillRepository;
    }

    public OilMillResponse createOilMill(OilMillRequest request) {
        String trimmedName = request.getName().trim();
        if (oilMillRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new IllegalArgumentException("An oil mill with this name already exists: " + trimmedName);
        }

        Instant now = Instant.now();
        OilMill mill = new OilMill();
        mill.setName(trimmedName);
        mill.setAddress(request.getAddress() != null && !request.getAddress().isBlank() ? request.getAddress().trim() : null);
        mill.setLatitude(request.getLatitude());
        mill.setLongitude(request.getLongitude());
        mill.setDailyCapacityKg(request.getDailyCapacityKg());
        mill.setActive(true);
        mill.setCreatedAt(now);
        mill.setUpdatedAt(now);

        return OilMillResponse.fromEntity(oilMillRepository.save(mill));
    }

    public List<OilMillResponse> getOilMills(Boolean active) {
        List<OilMill> mills = (active != null)
                ? oilMillRepository.findByActive(active)
                : oilMillRepository.findAll();

        return mills.stream()
                .map(OilMillResponse::fromEntity)
                .toList();
    }

    public OilMillResponse getOilMillById(String id) {
        return OilMillResponse.fromEntity(findOilMill(id));
    }

    public OilMillResponse updateOilMill(String id, OilMillRequest request) {
        OilMill mill = findOilMill(id);
        String trimmedName = request.getName().trim();
        if (oilMillRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, id)) {
            throw new IllegalArgumentException("An oil mill with this name already exists: " + trimmedName);
        }

        mill.setName(trimmedName);
        mill.setAddress(request.getAddress() != null && !request.getAddress().isBlank() ? request.getAddress().trim() : null);
        mill.setLatitude(request.getLatitude());
        mill.setLongitude(request.getLongitude());
        mill.setDailyCapacityKg(request.getDailyCapacityKg());
        mill.setUpdatedAt(Instant.now());

        return OilMillResponse.fromEntity(oilMillRepository.save(mill));
    }

    public OilMillResponse updateOilMillStatus(String id, boolean active) {
        OilMill mill = findOilMill(id);
        mill.setActive(active);
        mill.setUpdatedAt(Instant.now());

        return OilMillResponse.fromEntity(oilMillRepository.save(mill));
    }

    public OilMill findOilMill(String id) {
        return oilMillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Oil mill not found with id: " + id));
    }
}