package ISIMS.olive_connect.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Date;
import java.util.List;

@Document(collection = "users")
public class User implements UserDetails {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String password;
    private String fullName;
    private String phoneNumber;

    private Role role;

    // Two-step activation flags
    private boolean emailVerified = false;
    private boolean adminApproved = false;

    private String verificationCode;
    private Date verificationCodeExpiresAt;

    // Add inside ISIMS.olive_connect.entity.User
    private String resetPasswordCode;
    private Date resetPasswordCodeExpiresAt;

    // Getters and Setters
    public String getResetPasswordCode() { return resetPasswordCode; }
    public void setResetPasswordCode(String resetPasswordCode) { this.resetPasswordCode = resetPasswordCode; }

    public Date getResetPasswordCodeExpiresAt() { return resetPasswordCodeExpiresAt; }
    public void setResetPasswordCodeExpiresAt(Date resetPasswordCodeExpiresAt) { this.resetPasswordCodeExpiresAt = resetPasswordCodeExpiresAt; }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @Override
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public boolean isAdminApproved() { return adminApproved; }
    public void setAdminApproved(boolean adminApproved) { this.adminApproved = adminApproved; }

    public String getVerificationCode() { return verificationCode; }
    public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }

    public Date getVerificationCodeExpiresAt() { return verificationCodeExpiresAt; }
    public void setVerificationCodeExpiresAt(Date verificationCodeExpiresAt) { this.verificationCodeExpiresAt = verificationCodeExpiresAt; }

    // Spring Security checks this during login: requires BOTH email verification AND admin approval
    @Override
    public boolean isEnabled() {
        return this.emailVerified && this.adminApproved;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role == null) {
            return List.of();
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() { return email; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }
}