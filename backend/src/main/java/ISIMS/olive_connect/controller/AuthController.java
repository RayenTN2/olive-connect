package ISIMS.olive_connect.controller;

import ISIMS.olive_connect.dto.ApiResponse;
import ISIMS.olive_connect.dto.RegisterRequest;
import ISIMS.olive_connect.dto.VerifyAccountRequest;
import ISIMS.olive_connect.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}