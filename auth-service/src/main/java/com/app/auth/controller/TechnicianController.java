package com.app.auth.controller;

import com.app.auth.dto.response.TechnicianProfileResponseDTO;
import com.app.auth.payload.ApiResponse;
import com.app.auth.service.TechnicianProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/technician")
@RequiredArgsConstructor
public class TechnicianController {

    private final TechnicianProfileService technicianProfileService;

    @GetMapping("/profile")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<ApiResponse<TechnicianProfileResponseDTO>> getMyProfile(
            Authentication authentication
    ) {
        String username = authentication.getName();
        TechnicianProfileResponseDTO profile =
                technicianProfileService.getProfileByUsername(username);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Profile retrieved", profile)
        );
    }

    @PatchMapping("/profile/availability")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<ApiResponse<Void>> updateAvailability(
            @RequestParam boolean available,
            Authentication authentication
    ) {
        String username = authentication.getName();
        technicianProfileService.updateAvailability(username, available);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Availability updated", null)
        );
    }
}