package ISIMS.olive_connect.service;

import ISIMS.olive_connect.dto.CreateHarvestRequestDto;
import ISIMS.olive_connect.dto.HarvestRequestResponse;
import ISIMS.olive_connect.entity.HarvestRequest;
import ISIMS.olive_connect.entity.HarvestRequestStatus;
import ISIMS.olive_connect.entity.HarvestRequestType;
import ISIMS.olive_connect.entity.Orchard;
import ISIMS.olive_connect.entity.OrchardHarvestStatus;
import ISIMS.olive_connect.entity.User;
import ISIMS.olive_connect.repository.HarvestRequestRepository;
import ISIMS.olive_connect.repository.OrchardRepository;
import ISIMS.olive_connect.repository.UserRepository;
import org.springframework.stereotype.Service;
import ISIMS.olive_connect.entity.Role;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class HarvestRequestService {

    private static final List<HarvestRequestStatus> ACTIVE_STATUSES = List.of(
            HarvestRequestStatus.PENDING,
            HarvestRequestStatus.APPROVED,
            HarvestRequestStatus.SCHEDULED,
            HarvestRequestStatus.IN_PROGRESS
    );

    private final HarvestRequestRepository harvestRequestRepository;
    private final OrchardRepository orchardRepository;
    private final UserRepository userRepository;

    public HarvestRequestService(HarvestRequestRepository harvestRequestRepository,
                                 OrchardRepository orchardRepository,
                                 UserRepository userRepository) {
        this.harvestRequestRepository = harvestRequestRepository;
        this.orchardRepository = orchardRepository;
        this.userRepository = userRepository;
    }

    public HarvestRequestResponse createHarvestRequest(CreateHarvestRequestDto dto, String farmerEmail) {
        User farmer = userRepository.findByEmail(farmerEmail)
                .orElseThrow(() -> new RuntimeException("Farmer not found"));

        Orchard orchard = orchardRepository.findById(dto.getOrchardId())
                .orElseThrow(() -> new RuntimeException("Orchard not found with id: " + dto.getOrchardId()));

        if (!orchard.getFarmerId().equals(farmer.getId())) {
            throw new SecurityException("Access denied: You can only submit harvest requests for your own orchards.");
        }

        if (harvestRequestRepository.existsByOrchardIdAndStatusIn(orchard.getId(), ACTIVE_STATUSES)) {
            throw new IllegalArgumentException("An active harvest request already exists for this orchard.");
        }

        if (dto.getRequestType() == HarvestRequestType.PLANNED && dto.getPreferredDate() == null) {
            throw new IllegalArgumentException("Preferred date is required for a PLANNED harvest request.");
        }

        Instant now = Instant.now();

        HarvestRequest request = new HarvestRequest();
        request.setOrchardId(orchard.getId());
        request.setFarmerId(farmer.getId());
        request.setRequestType(dto.getRequestType());
        request.setPreferredDate(dto.getPreferredDate());
        request.setEstimatedQuantityKg(dto.getEstimatedQuantityKg());
        request.setNotes(dto.getNotes() != null ? dto.getNotes().trim() : null);
        request.setStatus(HarvestRequestStatus.PENDING);
        request.setCreatedAt(now);
        request.setUpdatedAt(now);

        HarvestRequest savedRequest = harvestRequestRepository.save(request);

        orchard.setHarvestStatus(OrchardHarvestStatus.REQUESTED);
        orchard.setUpdatedAt(now);
        orchardRepository.save(orchard);

        return HarvestRequestResponse.fromEntity(savedRequest, orchard);
    }

    public List<HarvestRequestResponse> getMyHarvestRequests(String farmerEmail) {
        User farmer = userRepository.findByEmail(farmerEmail)
                .orElseThrow(() -> new RuntimeException("Farmer not found"));

        List<HarvestRequest> requests = harvestRequestRepository.findByFarmerIdOrderByCreatedAtDesc(farmer.getId());
        return mapWithOrchards(requests);
    }

    private List<HarvestRequestResponse> mapWithOrchards(List<HarvestRequest> requests) {
        Set<String> orchardIds = requests.stream()
                .map(HarvestRequest::getOrchardId)
                .collect(Collectors.toSet());

        Map<String, Orchard> orchardMap = orchardRepository.findAllById(orchardIds)
                .stream()
                .collect(Collectors.toMap(Orchard::getId, Function.identity()));

        return requests.stream()
                .map(req -> HarvestRequestResponse.fromEntity(req, orchardMap.get(req.getOrchardId())))
                .toList();
    }

    public HarvestRequestResponse getHarvestRequestById(String requestId, String userEmail) {
    User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));

    HarvestRequest request = harvestRequestRepository.findById(requestId)
            .orElseThrow(() -> new RuntimeException("Harvest request not found with id: " + requestId));

    if (user.getRole() == Role.FARMER && !request.getFarmerId().equals(user.getId())) {
        throw new SecurityException("Access denied: You cannot consult another farmer's harvest request.");
    }

    Orchard orchard = orchardRepository.findById(request.getOrchardId()).orElse(null);
    return HarvestRequestResponse.fromEntity(request, orchard);
}

    
}