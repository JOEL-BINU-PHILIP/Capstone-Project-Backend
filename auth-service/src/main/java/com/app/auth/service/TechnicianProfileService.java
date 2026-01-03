package com.app.auth.service;

import com.app.auth.dto.response.TechnicianProfileResponseDTO;
import com.app.auth.model.TechnicianProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Set;

public interface TechnicianProfileService {

    TechnicianProfile createProfile(
            String userId,
            Set<String> skills,
            Integer experienceYears,
            String bio,
            String city,
            String state,
            String idProofType
    );

    TechnicianProfileResponseDTO getProfileByUsername(String username);

    TechnicianProfileResponseDTO getProfileById(String technicianId);

    void updateAvailability(String username, boolean available);

    void approveTechnician(String technicianId);

    void rejectTechnician(String technicianId, String reason);

    Page<TechnicianProfileResponseDTO> getTechniciansByStatus(
            TechnicianProfile.ApprovalStatus status,
            Boolean available,
            Pageable pageable
    );

    Page<TechnicianProfileResponseDTO> getAllTechnicians(Pageable pageable);
}