package ISIMS.olive_connect.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Document(collection = "users")
public class User implements UserDetails {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String passwordHash;
    private String fullName;
    private String phone;
    private String cin;

    @Indexed
    private Role role;

    @Indexed
    private AccountStatus status = AccountStatus.PENDING_APPROVAL;

    private String rejectionReason;

    private Instant createdAt;
    private Instant updatedAt;

    // Email verification & password reset fields
    private boolean emailVerified = false;
    private String verificationCode;
    private Date verificationCodeExpiresAt;

    private String resetPasswordCode;
    private Date resetPasswordCodeExpiresAt;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

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

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public String getVerificationCode() { return verificationCode; }
    public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }

    public Date getVerificationCodeExpiresAt() { return verificationCodeExpiresAt; }
    public void setVerificationCodeExpiresAt(Date verificationCodeExpiresAt) { this.verificationCodeExpiresAt = verificationCodeExpiresAt; }

    public String getResetPasswordCode() { return resetPasswordCode; }
    public void setResetPasswordCode(String resetPasswordCode) { this.resetPasswordCode = resetPasswordCode; }

    public Date getResetPasswordCodeExpiresAt() { return resetPasswordCodeExpiresAt; }
    public void setResetPasswordCodeExpiresAt(Date resetPasswordCodeExpiresAt) { this.resetPasswordCodeExpiresAt = resetPasswordCodeExpiresAt; }

    // --- Spring Security UserDetails Implementation ---

    @Override
    public String getPassword() {
        return this.passwordHash;
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isEnabled() {
        return this.emailVerified && this.status == AccountStatus.ACTIVE;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.status != AccountStatus.SUSPENDED;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role == null) {
            return List.of();
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }
}