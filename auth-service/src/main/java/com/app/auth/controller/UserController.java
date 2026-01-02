package com.app. auth.controller;

import com.app. auth.dto.request.UpdateProfileRequest;
import com.app.auth. dto.response.UserProfileResponseDTO;
import com. app.auth.payload.ApiResponse;
import com.app.auth. service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok. extern.slf4j.Slf4j;
import org. springframework.http.ResponseEntity;
import org.springframework.security. access.prepost. PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Get current user's profile
     * Works for all authenticated users (Customer, Technician, Manager, Admin)
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserProfileResponseDTO>> getCurrentUser(
            Authentication authentication
    ) {
        String username = authentication.getName();
        log.debug("Getting profile for user: {}", username);

        UserProfileResponseDTO profile = userService.getProfileByUsername(username);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Profile retrieved", profile)
        );
    }

    /**
     * Update current user's profile
     * Works for all authenticated users
     */
    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserProfileResponseDTO>> updateCurrentUser(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication
    ) {
        String username = authentication.getName();
        log.debug("Updating profile for user: {}", username);

        UserProfileResponseDTO profile = userService. updateProfile(username, request);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Profile updated successfully", profile)
        );
    }

    /**
     * Change password for current user
     */
    @PutMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication
    ) {
        String username = authentication. getName();
        log.debug("Changing password for user: {}", username);

        userService. changePassword(username, request);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Password changed successfully", null)
        );
    }

    /**
     * Delete/Deactivate user (Admin only)
     */
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable String userId
    ) {
        log.info("Admin deleting/deactivating user:  {}", userId);

        userService.deactivateUser(userId);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "User deactivated successfully", null)
        );
    }
}