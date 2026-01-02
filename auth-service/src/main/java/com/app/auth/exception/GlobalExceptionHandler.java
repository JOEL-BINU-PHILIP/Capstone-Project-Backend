package com.app.auth.exception;

import com.app.auth.payload.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ===============================
    // Validation errors
    // ===============================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationErrors(
            MethodArgumentNotValidException ex
    ) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("Validation error: {}", message);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(false, message, null));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException ex
    ) {
        log.warn("Constraint violation: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(false, ex.getMessage(), null));
    }

    // ===============================
    // Authentication errors
    // ===============================
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCredentials(
            InvalidCredentialsException ex
    ) {
        log.warn("Invalid credentials attempt");

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse<>(false, ex.getMessage(), null));
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleAccountLocked(
            AccountLockedException ex
    ) {
        log.warn("Account locked: {}", ex.getMessage());

        Map<String, Object> data = new HashMap<>();
        if (ex.getLockedUntil() != null) {
            long minutesRemaining = Duration.between(
                    Instant.now(),
                    ex.getLockedUntil()
            ).toMinutes();
            data.put("lockedUntil", ex.getLockedUntil());
            data.put("minutesRemaining", minutesRemaining);
        }

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse<>(false, ex.getMessage(), data));
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmailNotVerified(
            EmailNotVerifiedException ex
    ) {
        log.warn("Email not verified: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse<>(false, ex.getMessage(), null));
    }

    // ===============================
    // Token errors
    // ===============================
    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ApiResponse<Void>> handleTokenExpired(
            TokenExpiredException ex
    ) {
        log.warn("Token expired: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse<>(false, ex.getMessage(), null));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidToken(
            InvalidTokenException ex
    ) {
        log.warn("Invalid token: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse<>(false, ex.getMessage(), null));
    }

    // ===============================
    // Resource errors
    // ===============================
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(
            ResourceNotFoundException ex
    ) {
        log.warn("Resource not found: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(false, ex.getMessage(), null));
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserAlreadyExists(
            UserAlreadyExistsException ex
    ) {
        log.warn("User already exists: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ApiResponse<>(false, ex.getMessage(), null));
    }

    // ===============================
    // Rate limiting
    // ===============================
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiResponse<Map<String, Long>>> handleRateLimitExceeded(
            RateLimitExceededException ex
    ) {
        log.warn("Rate limit exceeded: {}", ex.getMessage());

        Map<String, Long> data = new HashMap<>();
        data.put("retryAfterSeconds", ex.getRetryAfterSeconds());

        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(new ApiResponse<>(false, ex.getMessage(), data));
    }

    // ===============================
    // Spring Security exceptions
    // ===============================
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
            AuthenticationException ex
    ) {
        log.warn("Authentication failed: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse<>(false, "Authentication failed", null));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
            AccessDeniedException ex
    ) {
        log.warn("Access denied: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse<>(false, "Access denied", null));
    }

    // ===============================
    // Generic errors
    // ===============================
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(
            IllegalArgumentException ex
    ) {
        log.warn("Illegal argument: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(false, ex.getMessage(), null));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(
            IllegalStateException ex
    ) {
        log.warn("Illegal state: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ApiResponse<>(false, ex.getMessage(), null));
    }

    // ===============================
    // Fallback
    // ===============================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(
            Exception ex
    ) {
        log.error("Unexpected error", ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(
                        false,
                        "An unexpected error occurred. Please try again later.",
                        null
                ));
    }

    // ===============================
// Technician Approval Exception
// ===============================
    @ExceptionHandler(TechnicianNotApprovedException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleTechnicianNotApproved(
            TechnicianNotApprovedException ex
    ) {
        log.warn("Technician login attempt without approval: {}", ex.getMessage());

        Map<String, Object> data = new HashMap<>();
        data.put("status", ex.getStatus().name());
        data.put("requiresApproval", true);

        if (ex.getRejectionReason() != null) {
            data. put("rejectionReason", ex.getRejectionReason());
        }

        HttpStatus httpStatus;
        switch (ex.getStatus()) {
            case PENDING:
                httpStatus = HttpStatus.FORBIDDEN; // 403
                data.put("nextStep", "Wait for service manager approval.  You will receive an email once approved.");
                break;
            case REJECTED:
                httpStatus = HttpStatus.FORBIDDEN; // 403
                data.put("nextStep", "Your application was rejected. Please contact support for more information.");
                break;
            case SUSPENDED:
                httpStatus = HttpStatus.FORBIDDEN; // 403
                data.put("nextStep", "Your account has been suspended.  Please contact support.");
                break;
            default:
                httpStatus = HttpStatus.FORBIDDEN;
                data.put("nextStep", "Please contact support.");
        }

        return ResponseEntity
                .status(httpStatus)
                .body(new ApiResponse<>(false, ex.getMessage(), data));
    }
}