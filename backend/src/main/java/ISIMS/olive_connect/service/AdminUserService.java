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
    private final RefreshTokenService refreshTokenService;

    public AdminUserService(UserRepository userRepository,
                            PasswordEncoder passwordEncoder,
                            RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
    }

    // 1. List all verified farmers awaiting approval (US-02)
    public List<UserSummaryResponse> getPendingFarmers() {
        return userRepository
                .findByRoleAndEmailVerifiedTrueAndStatus(Role.FARMER, AccountStatus.PENDING_APPROVAL)
                .stream()
                .map(UserSummaryResponse::fromEntity)
                .toList();
    }

    // 2. Approve a farmer registration request (US-02)
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

    // 3. Reject a farmer registration request with a reason (US-02)
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

    // 4. Create an internal cooperative staff account (US-03)
    public UserSummaryResponse createStaffUser(CreateStaffRequest request) {
        if (request.getRole() == Role.FARMER) {
            throw new SecurityException("Staff creation endpoint cannot be used for farmers. Use public registration for farmers.");
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
        staff.setEmailVerified(true);
        staff.setStatus(AccountStatus.ACTIVE);
        staff.setCreatedAt(now);
        staff.setUpdatedAt(now);

        User savedStaff = userRepository.save(staff);
        return UserSummaryResponse.fromEntity(savedStaff);
    }

    // 5. List all cooperative users with optional role and status filters (US-05)
    public List<UserSummaryResponse> getAllUsers(Role role, AccountStatus status) {
        List<User> users;

        if (role != null && status != null) {
            users = userRepository.findByRoleAndStatus(role, status);
        } else if (role != null) {
            users = userRepository.findByRole(role);
        } else if (status != null) {
            users = userRepository.findByStatus(status);
        } else {
            users = userRepository.findAll();
        }

        return users.stream()
                .map(UserSummaryResponse::fromEntity)
                .toList();
    }

    // 6. Update a user's Role (US-05)
    public UserSummaryResponse updateUserRole(String userId, Role newRole, String currentAdminEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        if (user.getEmail().equalsIgnoreCase(currentAdminEmail) && newRole != Role.ADMIN) {
            throw new SecurityException("Administrators cannot remove their own ADMIN role.");
        }

        user.setRole(newRole);
        user.setUpdatedAt(Instant.now());
        User updatedUser = userRepository.save(user);

        // Revoke refresh tokens so the user must re-authenticate with their new role permissions
        refreshTokenService.revokeToken(user.getEmail());

        return UserSummaryResponse.fromEntity(updatedUser);
    }

    // 7. Suspend or reactivate an account (US-05)
    public UserSummaryResponse updateUserStatus(String userId, AccountStatus newStatus, String currentAdminEmail) {
        if (newStatus != AccountStatus.ACTIVE && newStatus != AccountStatus.SUSPENDED) {
            throw new RuntimeException("Allowed status transitions for this endpoint are ACTIVE or SUSPENDED.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        if (user.getEmail().equalsIgnoreCase(currentAdminEmail) && newStatus == AccountStatus.SUSPENDED) {
            throw new SecurityException("Administrators cannot suspend their own account.");
        }

        user.setStatus(newStatus);
        if (newStatus == AccountStatus.ACTIVE) {
            user.setRejectionReason(null);
        }
        user.setUpdatedAt(Instant.now());
        User updatedUser = userRepository.save(user);

        // Immediately revoke active sessions if suspended
        if (newStatus == AccountStatus.SUSPENDED) {
            refreshTokenService.revokeToken(user.getEmail());
        }

        return UserSummaryResponse.fromEntity(updatedUser);
    }
}