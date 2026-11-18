package ISIMS.olive_connect.service;

import ISIMS.olive_connect.dto.OrchardResponse;
import ISIMS.olive_connect.dto.RegisterOrchardRequest;
import ISIMS.olive_connect.dto.UpdateOrchardRequest;
import ISIMS.olive_connect.entity.Orchard;
import ISIMS.olive_connect.entity.OrchardHarvestStatus;
import ISIMS.olive_connect.entity.User;
import ISIMS.olive_connect.repository.OrchardRepository;
import ISIMS.olive_connect.repository.UserRepository;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class OrchardService {

    private final OrchardRepository orchardRepository;
    private final UserRepository userRepository;

    public OrchardService(OrchardRepository orchardRepository, UserRepository userRepository) {
        this.orchardRepository = orchardRepository;
        this.userRepository = userRepository;
    }

    public OrchardResponse registerOrchard(RegisterOrchardRequest request, String farmerEmail) {
        User farmer = userRepository.findByEmail(farmerEmail)
                .orElseThrow(() -> new RuntimeException("Farmer not found"));

        Instant now = Instant.now();
        Orchard orchard = new Orchard();
        orchard.setFarmerId(farmer.getId());
        orchard.setName(request.getName());
        orchard.setSurfaceAreaHectares(request.getSurfaceAreaHectares());
        orchard.setTreeCount(request.getTreeCount());
        orchard.setOliveVarieties(request.getOliveVarieties());
        
        // MongoDB GeoJsonPoint constructor takes (longitude, latitude)
        orchard.setLocation(new GeoJsonPoint(request.getLongitude(), request.getLatitude()));
        
        orchard.setCreatedAt(now);
        orchard.setUpdatedAt(now);

        Orchard savedOrchard = orchardRepository.save(orchard);
        return OrchardResponse.fromEntity(savedOrchard);
    }

    public List<OrchardResponse> getFarmerOrchards(String farmerEmail) {
        User farmer = userRepository.findByEmail(farmerEmail)
                .orElseThrow(() -> new RuntimeException("Farmer not found"));

        return orchardRepository.findByFarmerId(farmer.getId())
                .stream()
                .map(OrchardResponse::fromEntity)
                .toList();
    }

    // US-09: View single orchard details (owner verification)
    public OrchardResponse getOrchardById(String orchardId, String farmerEmail) {
        User farmer = userRepository.findByEmail(farmerEmail)
                .orElseThrow(() -> new RuntimeException("Farmer not found"));

        Orchard orchard = orchardRepository.findById(orchardId)
                .orElseThrow(() -> new RuntimeException("Orchard not found with id: " + orchardId));

        if (!orchard.getFarmerId().equals(farmer.getId())) {
            throw new SecurityException("Access denied: You do not own this orchard.");
        }

        return OrchardResponse.fromEntity(orchard);
    }

    // US-09: Update orchard information and location
    public OrchardResponse updateOrchard(String orchardId, UpdateOrchardRequest request, String farmerEmail) {
        User farmer = userRepository.findByEmail(farmerEmail)
                .orElseThrow(() -> new RuntimeException("Farmer not found"));

        Orchard orchard = orchardRepository.findById(orchardId)
                .orElseThrow(() -> new RuntimeException("Orchard not found with id: " + orchardId));

        if (!orchard.getFarmerId().equals(farmer.getId())) {
            throw new SecurityException("Access denied: You do not own this orchard.");
        }

        if (orchard.getHarvestStatus() == OrchardHarvestStatus.IN_PROGRESS) {
            throw new RuntimeException("Cannot modify orchard characteristics while a harvest is IN_PROGRESS.");
        }

        orchard.setName(request.getName());
        orchard.setSurfaceAreaHectares(request.getSurfaceAreaHectares());
        orchard.setTreeCount(request.getTreeCount());
        orchard.setOliveVarieties(request.getOliveVarieties());
        orchard.setLocation(new GeoJsonPoint(request.getLongitude(), request.getLatitude()));
        orchard.setUpdatedAt(Instant.now());

        Orchard updatedOrchard = orchardRepository.save(orchard);
        return OrchardResponse.fromEntity(updatedOrchard);
    }
}