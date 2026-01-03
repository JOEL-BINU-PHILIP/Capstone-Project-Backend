package com.app.auth.service.impl;

import com.app.auth.dto.response.TechnicianProfileResponseDTO;
import com.app.auth.exception.ResourceNotFoundException;
import com.app.auth.model.TechnicianProfile;
import com.app.auth.model.User;
import com.app.auth.repository.TechnicianProfileRepository;
import com.app.auth.repository.UserRepository;
import com.app.auth.service.EmailService;
import com.app.auth.service.TechnicianProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class TechnicianProfileServiceImpl implements TechnicianProfileService {

    private final TechnicianProfileRepository technicianProfileRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Override
    @Transactional
    public TechnicianProfile createProfile(
            String userId,
            Set<String> skills,
            Integer experienceYears,
            String bio,
            String city,
            String state,
            String idProofType
    ) {
        if (technicianProfileRepository.existsByUserId(userId)) {
            throw new IllegalStateException("Technician profile already exists");
        }

        TechnicianProfile profile = TechnicianProfile.builder()
                .userId(userId)
                .skills(skills)
                .experienceYears(experienceYears)
                .bio(bio)
                .city(city)
                .state(state)
                .idProofType(idProofType)
                .approvalStatus(TechnicianProfile.ApprovalStatus.PENDING)
                .available(false) // Not available until approved
                .build();

        return technicianProfileRepository.save(profile);
    }

    @Override
    public TechnicianProfileResponseDTO getProfileByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        TechnicianProfile profile = technicianProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Technician profile not found"));

        return mapToDTO(profile, user);
    }

    @Override
    @Transactional
    public void updateAvailability(String username, boolean available) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        TechnicianProfile profile = technicianProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Technician profile not found"));

        if (profile.getApprovalStatus() != TechnicianProfile.ApprovalStatus.APPROVED) {
            throw new IllegalStateException("Cannot update availability. Profile not approved");
        }

        profile.setAvailable(available);
        technicianProfileRepository.save(profile);

        log.info("Technician {} availability updated to: {}", username, available);
    }

    @Override
    @Transactional
    public void approveTechnician(String technicianId) {
        TechnicianProfile profile = technicianProfileRepository.findById(technicianId)
                .orElseThrow(() -> new ResourceNotFoundException("Technician profile not found"));

        if (profile.getApprovalStatus() == TechnicianProfile.ApprovalStatus.APPROVED) {
            throw new IllegalStateException("Technician is already approved");
        }

        profile.setApprovalStatus(TechnicianProfile.ApprovalStatus.APPROVED);
        profile.setApprovedAt(Instant.now());
        profile.setAvailable(true); // Make available by default
        technicianProfileRepository.save(profile);

        // Send approval email
        User user = userRepository.findById(profile.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        emailService.sendWelcomeEmail(user.getEmail(), user.getUsername());

        log.info("Technician approved: {}", user.getUsername());
    }

    @Override
    @Transactional
    public void rejectTechnician(String technicianId, String reason) {
        TechnicianProfile profile = technicianProfileRepository.findById(technicianId)
                .orElseThrow(() -> new ResourceNotFoundException("Technician profile not found"));

        profile.setApprovalStatus(TechnicianProfile.ApprovalStatus.REJECTED);
        profile.setRejectionReason(reason);
        technicianProfileRepository.save(profile);

        log.info("Technician rejected: {} - Reason: {}", technicianId, reason);
    }

    @Override
    public Page<TechnicianProfileResponseDTO> getTechniciansByStatus(
            TechnicianProfile.ApprovalStatus status,
            Boolean available,
            Pageable pageable
    ) {
        Page<TechnicianProfile> profiles;

        // Check if 'available' filter is applied
        if (available != null) {
            profiles = technicianProfileRepository.findByApprovalStatusAndAvailable(status, available, pageable);
        } else {
            // Fallback to original behavior (ignore availability)
            profiles = technicianProfileRepository.findByApprovalStatus(status, pageable);
        }

        return profiles.map(profile -> {
            User user = userRepository.findById(profile.getUserId()).orElse(null);
            return mapToDTO(profile, user);
        });
    }

    @Override
    public Page<TechnicianProfileResponseDTO> getAllTechnicians(Pageable pageable) {
        Page<TechnicianProfile> profiles = technicianProfileRepository.findAll(pageable);

        return profiles.map(profile -> {
            User user = userRepository.findById(profile.getUserId()).orElse(null);
            return mapToDTO(profile, user);
        });
    }

    private TechnicianProfileResponseDTO mapToDTO(TechnicianProfile profile, User user) {
        if (user == null) {
            return null;
        }

        return TechnicianProfileResponseDTO.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .skills(profile.getSkills())
                .experienceYears(profile.getExperienceYears())
                .bio(profile.getBio())
                .city(profile.getCity())
                .state(profile.getState())
                .approvalStatus(profile.getApprovalStatus())
                .rejectionReason(profile.getRejectionReason())
                .averageRating(profile.getAverageRating())
                .totalJobsCompleted(profile.getTotalJobsCompleted())
                .available(profile.isAvailable())
                .createdAt(profile.getCreatedAt())
                .build();
    }

    @Override
    public TechnicianProfileResponseDTO getProfileById(String technicianId) {
        TechnicianProfile profile = technicianProfileRepository.findById(technicianId)
                .orElseThrow(() -> new ResourceNotFoundException("Technician profile not found"));

        User user = userRepository.findById(profile.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return mapToDTO(profile, user);
    }
}