package ISIMS.olive_connect.service;

import ISIMS.olive_connect.dto.RegisterRequest;
import ISIMS.olive_connect.entity.Role;
import ISIMS.olive_connect.entity.User;
import ISIMS.olive_connect.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import java.util.Random;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public String registerFarmer(RegisterRequest request) {
        // 1. Business Rule: Visitors can only request FARMER registration
        if (request.getRole() != Role.FARMER) {
            throw new SecurityException("Unauthorized role request! Visitors can only request registration as farmers.");
        }

        Optional<User> existingUserOpt = userRepository.findByEmail(request.getEmail());
        User user;

        if (existingUserOpt.isPresent()) {
            user = existingUserOpt.get();
            // Block re-registration if email is already verified (either active or already queued for admin review)
            if (user.isEmailVerified()) {
                throw new RuntimeException("Email is already in use!");
            }
        } else {
            user = new User();
            user.setEmail(request.getEmail());
        }

        // 2. Generate 6-digit verification code (15-min expiry)
        String verificationCode = String.format("%06d", new Random().nextInt(999999));
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 15);

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setRole(Role.FARMER);
        user.setEmailVerified(false);
        user.setAdminApproved(false); // Must be approved by Admin later
        user.setVerificationCode(verificationCode);
        user.setVerificationCodeExpiresAt(calendar.getTime());

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

        // Mark email as verified, but keep adminApproved = false
        user.setEmailVerified(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);
        userRepository.save(user);

        return "Email verified successfully! Your registration request is now pending administrator approval.";
    }
}