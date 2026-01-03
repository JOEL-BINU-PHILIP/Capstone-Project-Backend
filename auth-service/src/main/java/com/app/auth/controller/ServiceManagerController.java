package com.app.auth.controller;

import com.app.auth.dto.response.TechnicianProfileResponseDTO;
import com.app.auth.model.TechnicianProfile;
import com.app.auth.payload.ApiResponse;
import com.app.auth.service.TechnicianProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/service-manager")
@RequiredArgsConstructor
public class ServiceManagerController {

    private final TechnicianProfileService technicianProfileService;

    /**
     * Get pending technicians awaiting approval
     */
    @GetMapping("/technicians/pending")
    @PreAuthorize("hasRole('SERVICE_MANAGER')")
    public ResponseEntity<ApiResponse<Page<TechnicianProfileResponseDTO>>> getPendingTechnicians(
            Pageable pageable
    ) {
        // Pass null for available since we only care about PENDING status here
        Page<TechnicianProfileResponseDTO> technicians =
                technicianProfileService.getTechniciansByStatus(
                        TechnicianProfile.ApprovalStatus.PENDING,
                        null,
                        pageable
                );

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Pending technicians retrieved", technicians)
        );
    }

    /**
     * Approve a technician
     */
    @PostMapping("/technicians/{technicianId}/approve")
    @PreAuthorize("hasRole('SERVICE_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> approveTechnician(
            @PathVariable("technicianId") String technicianId
    ) {
        technicianProfileService.approveTechnician(technicianId);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Technician approved successfully", null)
        );
    }

    /**
     * Reject a technician
     */
    @PostMapping("/technicians/{technicianId}/reject")
    @PreAuthorize("hasRole('SERVICE_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> rejectTechnician(
            @PathVariable("technicianId") String technicianId,
            @RequestBody(required = false) RejectRequest request
    ) {
        String reason = (request != null && request.getReason() != null)
                ? request.getReason()
                : "No reason provided";

        technicianProfileService.rejectTechnician(technicianId, reason);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Technician rejected", null)
        );
    }

    /**
     * Get all technicians with optional status and availability filters
     * Example: GET /api/service-manager/technicians?status=APPROVED&available=true
     */
    @GetMapping("/technicians")
    @PreAuthorize("hasRole('SERVICE_MANAGER')")
    public ResponseEntity<ApiResponse<Page<TechnicianProfileResponseDTO>>> getAllTechnicians(
            @RequestParam(name = "status", required = false) TechnicianProfile.ApprovalStatus status,
            @RequestParam(name = "available", required = false) Boolean available,
            Pageable pageable
    ) {
        Page<TechnicianProfileResponseDTO> technicians;

        if (status != null) {
            // Pass the available param to the service
            technicians = technicianProfileService.getTechniciansByStatus(status, available, pageable);
        } else {
            // If you want to filter by available WITHOUT status, you might need another service method.
            // For now, this falls back to getting all technicians if status is not provided.
            technicians = technicianProfileService.getAllTechnicians(pageable);
        }

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Technicians retrieved", technicians)
        );
    }

    /**
     * Get a single technician by ID
     */
    @GetMapping("/technicians/{technicianId}")
    @PreAuthorize("hasRole('SERVICE_MANAGER')")
    public ResponseEntity<ApiResponse<TechnicianProfileResponseDTO>> getTechnicianById(
            @PathVariable("technicianId") String technicianId
    ) {
        TechnicianProfileResponseDTO technician = technicianProfileService.getProfileById(technicianId);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Technician retrieved", technician)
        );
    }

    // ==================== INNER CLASS FOR REQUEST BODY ====================

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RejectRequest {
        private String reason;
    }
}