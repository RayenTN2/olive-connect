package ISIMS.olive_connect.service;

import ISIMS.olive_connect.dto.CreateHarvestRequestDto;
import ISIMS.olive_connect.dto.HarvestRequestResponse;
import ISIMS.olive_connect.dto.ReviewHarvestRequestDto;
import ISIMS.olive_connect.entity.HarvestRequest;
import ISIMS.olive_connect.entity.HarvestRequestStatus;
import ISIMS.olive_connect.entity.HarvestRequestType;
import ISIMS.olive_connect.entity.Orchard;
import ISIMS.olive_connect.entity.OrchardHarvestStatus;
import ISIMS.olive_connect.entity.Role;
import ISIMS.olive_connect.entity.User;
import ISIMS.olive_connect.repository.HarvestRequestRepository;
import ISIMS.olive_connect.repository.OrchardRepository;
import ISIMS.olive_connect.repository.UserRepository;
import org.springframework.stereotype.Service;

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

        return HarvestRequestResponse.fromEntity(savedRequest, orchard, farmer);
    }

    public List<HarvestRequestResponse> getMyHarvestRequests(String farmerEmail) {
        User farmer = userRepository.findByEmail(farmerEmail)
                .orElseThrow(() -> new RuntimeException("Farmer not found"));

        List<HarvestRequest> requests = harvestRequestRepository.findByFarmerIdOrderByCreatedAtDesc(farmer.getId());
        return mapWithDetails(requests);
    }

    public List<HarvestRequestResponse> getHarvestRequestsForManager(HarvestRequestStatus status,
                                                                   HarvestRequestType requestType,
                                                                   String orchardId) {
        List<HarvestRequest> requests;
        if (requestType != null && status != null) {
            requests = harvestRequestRepository.findByRequestTypeAndStatusOrderByCreatedAtDesc(requestType, status);
        } else if (status != null) {
            requests = harvestRequestRepository.findByStatusOrderByCreatedAtDesc(status);
        } else if (requestType != null) {
            requests = harvestRequestRepository.findByRequestTypeOrderByCreatedAtDesc(requestType);
        } else {
            requests = harvestRequestRepository.findAllByOrderByCreatedAtDesc();
        }

        if (orchardId != null && !orchardId.isBlank()) {
            requests = requests.stream()
                    .filter(req -> orchardId.equals(req.getOrchardId()))
                    .toList();
        }

        return mapWithDetails(requests);
    }

    public HarvestRequestResponse getHarvestRequestById(String requestId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        HarvestRequest request = harvestRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Harvest request not found with id: " + requestId));

        if (user.getRole() == Role.FARMER) {
            if (!request.getFarmerId().equals(user.getId())) {
                throw new SecurityException("Access denied: You cannot consult another farmer's harvest request.");
            }
        } else if (user.getRole() != Role.COLLECTION_MANAGER && user.getRole() != Role.ADMIN) {
            throw new SecurityException("Access denied: Insufficient privileges to consult this harvest request.");
        }

        Orchard orchard = orchardRepository.findById(request.getOrchardId()).orElse(null);
        User farmer = userRepository.findById(request.getFarmerId()).orElse(null);
        return HarvestRequestResponse.fromEntity(request, orchard, farmer);
    }

    private List<HarvestRequestResponse> mapWithDetails(List<HarvestRequest> requests) {
        Set<String> orchardIds = requests.stream()
                .map(HarvestRequest::getOrchardId)
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toSet());

        Set<String> farmerIds = requests.stream()
                .map(HarvestRequest::getFarmerId)
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toSet());

        Map<String, Orchard> orchardMap = orchardRepository.findAllById(orchardIds)
                .stream()
                .collect(Collectors.toMap(Orchard::getId, Function.identity()));

        Map<String, User> farmerMap = userRepository.findAllById(farmerIds)
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return requests.stream()
                .map(req -> HarvestRequestResponse.fromEntity(
                        req,
                        orchardMap.get(req.getOrchardId()),
                        farmerMap.get(req.getFarmerId())
                ))
                .toList();
    }

    public HarvestRequestResponse reviewHarvestRequest(String requestId, ReviewHarvestRequestDto dto, String managerEmail) {
        User reviewer = userRepository.findByEmail(managerEmail)
                .orElseThrow(() -> new RuntimeException("Reviewer not found"));

        HarvestRequest request = harvestRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Harvest request not found with id: " + requestId));

        if (request.getStatus() != HarvestRequestStatus.PENDING) {
            throw new IllegalStateException("Only PENDING harvest requests can be accepted or rejected. Current status: " + request.getStatus());
        }

        Instant now = Instant.now();
        boolean accepted = Boolean.TRUE.equals(dto.getAccepted());

        request.setStatus(accepted ? HarvestRequestStatus.APPROVED : HarvestRequestStatus.REJECTED);
        request.setReviewComment(dto.getComment() != null && !dto.getComment().isBlank() ? dto.getComment().trim() : null);
        request.setReviewedBy(reviewer.getId());
        request.setReviewedAt(now);
        request.setUpdatedAt(now);

        HarvestRequest savedRequest = harvestRequestRepository.save(request);

        orchardRepository.findById(request.getOrchardId()).ifPresent(orchard -> {
            if (!accepted) {
                boolean hasOtherActive = harvestRequestRepository.existsByOrchardIdAndStatusIn(
                        orchard.getId(), ACTIVE_STATUSES);
                if (!hasOtherActive) {
                    orchard.setHarvestStatus(OrchardHarvestStatus.IDLE);
                    orchard.setUpdatedAt(now);
                    orchardRepository.save(orchard);
                }
            }
        });

        Orchard orchard = orchardRepository.findById(savedRequest.getOrchardId()).orElse(null);
        User farmer = userRepository.findById(savedRequest.getFarmerId()).orElse(null);
        return HarvestRequestResponse.fromEntity(savedRequest, orchard, farmer);
    }
}