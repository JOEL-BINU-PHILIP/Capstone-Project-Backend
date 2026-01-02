package com.app.auth.controller;

import com.app.auth. dto.response.TechnicianProfileResponseDTO;
import com. app.auth.payload.ApiResponse;
import com.app.auth. service.TechnicianProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org. springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/technician")
@RequiredArgsConstructor
public class TechnicianController {

    private final TechnicianProfileService technicianProfileService;

    /**
     * Get current technician's profile
     */
    @GetMapping("/profile")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<ApiResponse<TechnicianProfileResponseDTO>> getMyProfile(
            Authentication authentication
    ) {
        String username = authentication.getName();
        log.debug("Getting profile for technician: {}", username);

        TechnicianProfileResponseDTO profile =
                technicianProfileService.getProfileByUsername(username);

        return ResponseEntity. ok(
                new ApiResponse<>(true, "Profile retrieved", profile)
        );
    }

    /**
     * Update technician availability
     * Endpoint:  PATCH /api/technician/availability? available=true
     */
    @PatchMapping("/availability")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<ApiResponse<Void>> updateAvailability(
            @RequestParam("available") boolean available,
            Authentication authentication
    ) {
        String username = authentication. getName();
        log.info("Updating availability for technician: {} to {}", username, available);

        technicianProfileService.updateAvailability(username, available);

        return ResponseEntity. ok(
                new ApiResponse<>(true, "Availability updated to:  " + available, null)
        );
    }
}