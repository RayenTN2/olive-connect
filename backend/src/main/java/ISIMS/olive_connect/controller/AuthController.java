package ISIMS.olive_connect.controller;

import ISIMS.olive_connect.dto.ApiResponse;
import ISIMS.olive_connect.dto.ForgotPasswordRequest;
import ISIMS.olive_connect.dto.JwtResponse;
import ISIMS.olive_connect.dto.LoginRequest;
import ISIMS.olive_connect.dto.RegisterRequest;
import ISIMS.olive_connect.dto.ResetPasswordRequest;
import ISIMS.olive_connect.dto.VerifyAccountRequest;
import ISIMS.olive_connect.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegisterRequest request) {
        String message = authService.registerFarmer(request);
        return ResponseEntity
                .status(HttpStatus.CREATED) // Returns 201 Created
                .body(new ApiResponse(true, message));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse> verifyAccount(@Valid @RequestBody VerifyAccountRequest request) {
        String message = authService.verifyAccount(request.getEmail(), request.getCode());
        return ResponseEntity.ok(new ApiResponse(true, message)); // Keeps 200 OK
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> authenticateUser(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        JwtResponse jwtResponse = authService.login(request, response);
        return ResponseEntity.ok(jwtResponse);
    }
    
    @PostMapping("/refresh-token")
        public ResponseEntity<?> refreshToken(@CookieValue(name = "refreshToken", required = false) String refreshToken) {
        if (refreshToken == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Refresh token cookie is missing."));
        }
        String newAccessToken = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(Map.of("accessToken", newAccessToken, "tokenType", "Bearer"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String message = authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(new ApiResponse(true, message));
    }

    @PostMapping("/verify-reset-code")
    public ResponseEntity<ApiResponse> verifyResetCode(@RequestParam String email, @RequestParam String code) {
        String message = authService.verifyResetCode(email, code);
        return ResponseEntity.ok(new ApiResponse(true, message));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        String message = authService.resetPassword(request);
        return ResponseEntity.ok(new ApiResponse(true, message));
    }
}

