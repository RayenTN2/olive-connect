package ISIMS.olive_connect.exception;

import ISIMS.olive_connect.dto.ApiResponse;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Arrays;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Catches unauthorized role registration attempts (Business Rule)
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiResponse> handleSecurityException(SecurityException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN) // 403 with JSON body
                .body(new ApiResponse(false, ex.getMessage()));
    }

    // 1b. Catches @PreAuthorize denials (otherwise the RuntimeException handler below would turn them into 400)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse> handleAccessDeniedException(AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse(false, ex.getMessage()));
    }

    // 2. Catches duplicate email, invalid/expired verification codes
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST) // 400 with JSON body
                .body(new ApiResponse(false, ex.getMessage()));
    }

    // 3. Catches @Valid DTO errors (@NotBlank, @Email, @Size, @Pattern)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, errorMessage));
    }

    // 4. Catches missing body, malformed JSON, or invalid Enum values dynamically
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse> handleJsonParseException(HttpMessageNotReadableException ex) {
        String message = "Request body is missing or contains malformed JSON.";

        if (ex.getCause() instanceof InvalidFormatException invalidFormatEx && invalidFormatEx.getTargetType().isEnum()) {
            Object[] enumConstants = invalidFormatEx.getTargetType().getEnumConstants();
            message = "Invalid value '" + invalidFormatEx.getValue() + "'. Allowed values: " + Arrays.toString(enumConstants);
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, message));
    }

    // 5. Catches invalid query parameter types or invalid Enum values in request params (e.g., ?role=INVALID)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse> handleTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        String message = "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'.";

        if (ex.getRequiredType() != null && ex.getRequiredType().isEnum()) {
            Object[] enumConstants = ex.getRequiredType().getEnumConstants();
            message = "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'. Allowed values: " + Arrays.toString(enumConstants);
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, message));
    }

    // 6. Catches authentication failures (bad credentials, disabled/unapproved/rejected/suspended account)
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse> handleAuthenticationException(AuthenticationException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse(false, ex.getMessage()));
    }

    // 7. Catches token refresh errors (expired, revoked, missing in db)
    @ExceptionHandler(TokenRefreshException.class)
    public ResponseEntity<ApiResponse> handleTokenRefreshException(TokenRefreshException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED) // 401 Unauthorized
                .body(new ApiResponse(false, ex.getMessage()));
    }

    // 8. Catches invalid token errors
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiResponse> handleInvalidTokenException(InvalidTokenException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED) // 401 Unauthorized
                .body(new ApiResponse(false, ex.getMessage()));
    }
}