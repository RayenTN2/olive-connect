package ISIMS.olive_connect.service;

import ISIMS.olive_connect.dto.CreateStaffRequest;
import ISIMS.olive_connect.dto.UserSummaryResponse;
import ISIMS.olive_connect.entity.AccountStatus;
import ISIMS.olive_connect.entity.Role;
import ISIMS.olive_connect.entity.User;
import ISIMS.olive_connect.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 1. List all verified farmers awaiting approval
    public List<UserSummaryResponse> getPendingFarmers() {
        return userRepository
                .findByRoleAndEmailVerifiedTrueAndStatus(Role.FARMER, AccountStatus.PENDING_APPROVAL)
                .stream()
                .map(UserSummaryResponse::fromEntity)
                .toList();
    }

    // 2. Approve a farmer registration request
    public String approveUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        if (!user.isEmailVerified()) {
            throw new RuntimeException("Cannot approve user: email address is not verified yet.");
        }

        if (user.getStatus() == AccountStatus.ACTIVE) {
            return "User " + user.getEmail() + " is already active.";
        }

        user.setStatus(AccountStatus.ACTIVE);
        user.setRejectionReason(null);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        return "User " + user.getEmail() + " has been approved and is now ACTIVE.";
    }

    // 3. Reject a farmer registration request with a reason
    public String rejectUser(String userId, String rejectionReason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        if (user.getStatus() == AccountStatus.REJECTED) {
            return "User " + user.getEmail() + " is already rejected.";
        }

        user.setStatus(AccountStatus.REJECTED);
        user.setRejectionReason(rejectionReason);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        return "User registration for " + user.getEmail() + " has been rejected.";
    }

    // 4. Create an internal cooperative staff account (pre-verified and active)
    public UserSummaryResponse createStaffUser(CreateStaffRequest request) {
        if (request.getRole() == Role.FARMER || request.getRole() == Role.VISITOR) {
            throw new SecurityException("Staff creation endpoint cannot be used for farmers or visitors. Use public registration for farmers.");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email address is already in use: " + request.getEmail());
        }

        Instant now = Instant.now();
        User staff = new User();
        staff.setFullName(request.getFullName());
        staff.setEmail(request.getEmail());
        staff.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        staff.setPhone(request.getPhone());
        staff.setCin(request.getCin());
        staff.setRole(request.getRole());
        
        // Staff accounts bypass email verification and pending approval queues
        staff.setEmailVerified(true);
        staff.setStatus(AccountStatus.ACTIVE);
        staff.setCreatedAt(now);
        staff.setUpdatedAt(now);

        User savedStaff = userRepository.save(staff);
        return UserSummaryResponse.fromEntity(savedStaff);
    }
}