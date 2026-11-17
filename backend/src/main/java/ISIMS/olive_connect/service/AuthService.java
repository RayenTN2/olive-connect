package ISIMS.olive_connect.service;

import ISIMS.olive_connect.dto.JwtResponse;
import ISIMS.olive_connect.dto.LoginRequest;
import ISIMS.olive_connect.dto.RegisterRequest;
import ISIMS.olive_connect.dto.ResetPasswordRequest;
import ISIMS.olive_connect.entity.AccountStatus;
import ISIMS.olive_connect.entity.RefreshToken;
import ISIMS.olive_connect.entity.Role;
import ISIMS.olive_connect.entity.User;
import ISIMS.olive_connect.exception.TokenRefreshException;
import ISIMS.olive_connect.repository.UserRepository;
import ISIMS.olive_connect.security.JwtUtils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       EmailService emailService,
                       AuthenticationManager authenticationManager,
                       JwtUtils jwtUtils,
                       RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.refreshTokenService = refreshTokenService;
    }

    public String registerFarmer(RegisterRequest request) {
        // 1. Business Rule: Visitors can only request FARMER registration
        if (request.getRole() != Role.FARMER) {
            throw new SecurityException("Unauthorized role request! Visitors can only request registration as farmers.");
        }

        Optional<User> existingUserOpt = userRepository.findByEmail(request.getEmail());
        User user;
        Instant now = Instant.now();

        if (existingUserOpt.isPresent()) {
            user = existingUserOpt.get();
            // Block re-registration if email is already verified (either active or already queued for admin review)
            if (user.isEmailVerified()) {
                throw new RuntimeException("Email is already in use!");
            }
        } else {
            user = new User();
            user.setEmail(request.getEmail());
            user.setCreatedAt(now);
        }

        // 2. Generate 6-digit verification code (15-min expiry)
        String verificationCode = String.format("%06d", new SecureRandom().nextInt(1000000));
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 15);

        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setCin(request.getCin());
        user.setRole(Role.FARMER);
        user.setEmailVerified(false);
        user.setStatus(AccountStatus.PENDING_APPROVAL);
        user.setRejectionReason(null);
        user.setVerificationCode(verificationCode);
        user.setVerificationCodeExpiresAt(calendar.getTime());
        user.setUpdatedAt(now);

        userRepository.save(user);
        emailService.sendVerificationEmail(user.getEmail(), verificationCode);

        return "Registration request submitted! Please enter the verification code sent to your email.";
    }

    public String verifyAccount(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isEmailVerified()) {
            return "Email is already verified. Your account is awaiting administrator approval.";
        }

        if (user.getVerificationCode() == null || !user.getVerificationCode().equals(code)) {
            throw new RuntimeException("Invalid verification code.");
        }

        if (user.getVerificationCodeExpiresAt().before(new Date())) {
            throw new RuntimeException("Verification code has expired.");
        }

        // Mark email as verified, keep status = PENDING_APPROVAL for Admin review
        user.setEmailVerified(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        return "Email verified successfully! Your registration request is now pending administrator approval.";
    }

    public JwtResponse login(LoginRequest request, HttpServletResponse response) {
        // 1. Fetch user and verify password first so we don't leak account status to unauthenticated callers
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password.");
        }

        // 2. Check specific lifecycle states to return clear feedback
        if (!user.isEmailVerified()) {
            throw new DisabledException("Please verify your email address before logging in.");
        }

        if (user.getStatus() == AccountStatus.PENDING_APPROVAL) {
            throw new DisabledException("Your account is pending administrator approval.");
        }

        if (user.getStatus() == AccountStatus.REJECTED) {
            String reason = (user.getRejectionReason() != null && !user.getRejectionReason().isBlank())
                    ? user.getRejectionReason()
                    : "No reason provided.";
            throw new DisabledException("Your account registration was rejected. Reason: " + reason);
        }

        if (user.getStatus() == AccountStatus.SUSPENDED) {
            throw new LockedException("Your account has been suspended by the administrator.");
        }

        // 3. Authenticate via Spring Security AuthenticationManager
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // 4. Set authentication context
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 5. Generate short-lived Access Token (for client memory)
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String accessToken = jwtUtils.generateJwtToken(userDetails);

        // 6. Generate Refresh Token and store in MongoDB (revocable)
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails);

        // 7. Attach Refresh Token inside an HttpOnly cookie
        Cookie cookie = new Cookie("refreshToken", refreshToken.getToken());
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Set to true if using HTTPS in production
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 60 * 60); // 7 days expiration
        response.addCookie(cookie);

        return new JwtResponse(accessToken, user.getEmail(), user.getFullName(), user.getRole().name());
    }

    public String refreshToken(String incomingRefreshToken) {
        return refreshTokenService.findByToken(incomingRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getEmail)
                .map(email -> {
                    User user = userRepository.findByEmail(email)
                            .orElseThrow(() -> new TokenRefreshException("User not found"));
                    return jwtUtils.generateJwtToken(user);
                })
                .orElseThrow(() -> new TokenRefreshException("Refresh token is invalid or not found in database!"));
    }

    public String forgotPassword(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        // Security best practice: Don't reveal whether an email exists in the system
        if (userOpt.isEmpty()) {
            return "If an account exists with this email, a password reset code has been sent.";
        }

        User user = userOpt.get();

        // Generate 6-digit code (15-min expiry) using SecureRandom
        String resetCode = String.format("%06d", new SecureRandom().nextInt(1000000));
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 15);

        user.setResetPasswordCode(resetCode);
        user.setResetPasswordCodeExpiresAt(calendar.getTime());
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        emailService.sendPasswordResetEmail(user.getEmail(), resetCode);

        return "If an account exists with this email, a password reset code has been sent.";
    }

    public String verifyResetCode(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid email or verification code."));

        if (user.getResetPasswordCode() == null || !user.getResetPasswordCode().equals(code)) {
            throw new RuntimeException("Invalid verification code.");
        }

        if (user.getResetPasswordCodeExpiresAt().before(new Date())) {
            throw new RuntimeException("Verification code has expired.");
        }

        return "Code verified. You may now set a new password.";
    }

    public String resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or verification code."));

        if (user.getResetPasswordCode() == null || !user.getResetPasswordCode().equals(request.getCode())) {
            throw new RuntimeException("Invalid verification code.");
        }

        if (user.getResetPasswordCodeExpiresAt().before(new Date())) {
            throw new RuntimeException("Verification code has expired.");
        }

        // 1. Update password with BCrypt hash
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));

        // 2. Clear reset code so it cannot be reused
        user.setResetPasswordCode(null);
        user.setResetPasswordCodeExpiresAt(null);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        // 3. Revoke all active refresh tokens for this user (force re-login on all devices)
        refreshTokenService.revokeToken(user.getEmail());

        return "Password has been reset successfully. Please log in with your new password.";
    }

    public String logout(String incomingRefreshToken, HttpServletResponse response) {
        // 1. Delete the refresh token from MongoDB if present
        if (incomingRefreshToken != null && !incomingRefreshToken.isBlank()) {
            refreshTokenService.findByToken(incomingRefreshToken)
                    .ifPresent(token -> refreshTokenService.revokeToken(token.getEmail()));
        }

        // 2. Clear the HttpOnly cookie in the client's browser
        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Match your login cookie setting (true in production HTTPS)
        cookie.setPath("/");
        cookie.setMaxAge(0); // Instructs browser to delete the cookie immediately
        response.addCookie(cookie);

        // 3. Clear Spring Security context for the current thread
        SecurityContextHolder.clearContext();

        return "Logged out successfully.";
    }
}