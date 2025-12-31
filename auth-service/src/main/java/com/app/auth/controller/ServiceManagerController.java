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

    @GetMapping("/technicians/pending")
    @PreAuthorize("hasRole('SERVICE_MANAGER')")
    public ResponseEntity<ApiResponse<Page<TechnicianProfileResponseDTO>>> getPendingTechnicians(
            Pageable pageable
    ) {
        Page<TechnicianProfileResponseDTO> technicians =
                technicianProfileService.getTechniciansByStatus(
                        TechnicianProfile.ApprovalStatus.PENDING,
                        pageable
                );

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Pending technicians retrieved", technicians)
        );
    }

    @PostMapping("/technicians/{technicianId}/approve")
    @PreAuthorize("hasRole('SERVICE_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> approveTechnician(
            @PathVariable String technicianId
    ) {
        technicianProfileService.approveTechnician(technicianId);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Technician approved successfully", null)
        );
    }

    @PostMapping("/technicians/{technicianId}/reject")
    @PreAuthorize("hasRole('SERVICE_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> rejectTechnician(
            @PathVariable String technicianId,
            @RequestParam String reason
    ) {
        technicianProfileService.rejectTechnician(technicianId, reason);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Technician rejected", null)
        );
    }

    @GetMapping("/technicians")
    @PreAuthorize("hasRole('SERVICE_MANAGER')")
    public ResponseEntity<ApiResponse<Page<TechnicianProfileResponseDTO>>> getAllTechnicians(
            @RequestParam(required = false) TechnicianProfile.ApprovalStatus status,
            Pageable pageable
    ) {
        Page<TechnicianProfileResponseDTO> technicians;

        if (status != null) {
            technicians = technicianProfileService.getTechniciansByStatus(status, pageable);
        } else {
            technicians = technicianProfileService.getAllTechnicians(pageable);
        }

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Technicians retrieved", technicians)
        );
    }
}