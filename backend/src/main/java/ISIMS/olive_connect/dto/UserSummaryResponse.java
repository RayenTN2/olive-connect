package ISIMS.olive_connect.dto;

import ISIMS.olive_connect.entity.AccountStatus;
import ISIMS.olive_connect.entity.Role;
import ISIMS.olive_connect.entity.User;

import java.time.Instant;

public class UserSummaryResponse {

    private String id;
    private String email;
    private String fullName;
    private String phone;
    private String cin;
    private Role role;
    private AccountStatus status;
    private boolean emailVerified;
    private String rejectionReason;
    private Instant createdAt;
    private Instant updatedAt;

    public static UserSummaryResponse fromEntity(User user) {
        UserSummaryResponse dto = new UserSummaryResponse();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setPhone(user.getPhone());
        dto.setCin(user.getCin());
        dto.setRole(user.getRole());
        dto.setStatus(user.getStatus());
        dto.setEmailVerified(user.isEmailVerified());
        dto.setRejectionReason(user.getRejectionReason());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getCin() { return cin; }
    public void setCin(String cin) { this.cin = cin; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}