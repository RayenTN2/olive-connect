package ISIMS.olive_connect.controller;

import ISIMS.olive_connect.dto.ApiResponse;
import ISIMS.olive_connect.dto.RejectUserRequest;
import ISIMS.olive_connect.dto.UserSummaryResponse;
import ISIMS.olive_connect.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    // List verified farmers waiting for approval
    @GetMapping("/pending")
    public ResponseEntity<List<UserSummaryResponse>> getPendingUsers() {
        return ResponseEntity.ok(adminUserService.getPendingFarmers());
    }

    // Approve a registration request
    @PatchMapping("/{id}/approve")
    public ResponseEntity<ApiResponse> approveUser(@PathVariable String id) {
        String message = adminUserService.approveUser(id);
        return ResponseEntity.ok(new ApiResponse(true, message));
    }

    // Reject a registration request with a reason
    @PatchMapping("/{id}/reject")
    public ResponseEntity<ApiResponse> rejectUser(
            @PathVariable String id,
            @Valid @RequestBody RejectUserRequest request) {
        String message = adminUserService.rejectUser(id, request.getRejectionReason());
        return ResponseEntity.ok(new ApiResponse(true, message));
    }
}