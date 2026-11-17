package ISIMS.olive_connect.controller;

import ISIMS.olive_connect.dto.*;
import ISIMS.olive_connect.entity.AccountStatus;
import ISIMS.olive_connect.entity.Role;
import ISIMS.olive_connect.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    // US-02: List verified farmers waiting for approval
    @GetMapping("/pending")
    public ResponseEntity<List<UserSummaryResponse>> getPendingUsers() {
        return ResponseEntity.ok(adminUserService.getPendingFarmers());
    }

    // US-02: Approve a registration request
    @PatchMapping("/{id}/approve")
    public ResponseEntity<ApiResponse> approveUser(@PathVariable String id) {
        String message = adminUserService.approveUser(id);
        return ResponseEntity.ok(new ApiResponse(true, message));
    }

    // US-02: Reject a registration request with a reason
    @PatchMapping("/{id}/reject")
    public ResponseEntity<ApiResponse> rejectUser(
            @PathVariable String id,
            @Valid @RequestBody RejectUserRequest request) {
        String message = adminUserService.rejectUser(id, request.getRejectionReason());
        return ResponseEntity.ok(new ApiResponse(true, message));
    }

    // US-03: Create internal staff account
    @PostMapping("/staff")
    public ResponseEntity<UserSummaryResponse> createStaffUser(@Valid @RequestBody CreateStaffRequest request) {
        UserSummaryResponse createdStaff = adminUserService.createStaffUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdStaff);
    }

    // US-05: List all cooperative users (optionally filtered by ?role=...&status=...)
    @GetMapping
    public ResponseEntity<List<UserSummaryResponse>> getAllUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) AccountStatus status) {
        return ResponseEntity.ok(adminUserService.getAllUsers(role, status));
    }

    // US-05: Update a user's Role
    @PatchMapping("/{id}/role")
    public ResponseEntity<UserSummaryResponse> updateUserRole(
            @PathVariable String id,
            @Valid @RequestBody UpdateUserRoleRequest request,
            Authentication authentication) {
        UserSummaryResponse updated = adminUserService.updateUserRole(id, request.getRole(), authentication.getName());
        return ResponseEntity.ok(updated);
    }

    // US-05: Suspend or reactivate an account
    @PatchMapping("/{id}/status")
    public ResponseEntity<UserSummaryResponse> updateUserStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateUserStatusRequest request,
            Authentication authentication) {
        UserSummaryResponse updated = adminUserService.updateUserStatus(id, request.getStatus(), authentication.getName());
        return ResponseEntity.ok(updated);
    }
}