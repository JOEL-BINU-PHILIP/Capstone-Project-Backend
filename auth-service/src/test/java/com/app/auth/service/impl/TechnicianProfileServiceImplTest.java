package com.app.auth.service.impl;

import com.app.auth.dto.response.TechnicianProfileResponseDTO;
import com.app.auth.exception.ResourceNotFoundException;
import com.app.auth.model.TechnicianProfile;
import com.app.auth.model.User;
import com.app.auth.repository.TechnicianProfileRepository;
import com.app.auth.repository.UserRepository;
import com.app.auth.service.EmailService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TechnicianProfileServiceImplTest {

    @InjectMocks
    private TechnicianProfileServiceImpl technicianProfileService;

    @Mock
    private TechnicianProfileRepository technicianProfileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService; // 🔴 FIX

    // =========================
    // GET PROFILE BY USERNAME
    // =========================
    @Test
    void getProfileByUsername_success() {

        User user = User.builder()
                .id("u1")
                .username("tech1")
                .email("tech@test.com")
                .firstName("Tech")
                .lastName("One")
                .build();

        TechnicianProfile profile = TechnicianProfile.builder()
                .id("t1")
                .userId("u1")
                .skills(Set.of("PLUMBING"))
                .approvalStatus(TechnicianProfile.ApprovalStatus.APPROVED)
                .available(true)
                .createdAt(Instant.now())
                .build();

        when(userRepository.findByUsername("tech1"))
                .thenReturn(Optional.of(user));

        when(technicianProfileRepository.findByUserId("u1"))
                .thenReturn(Optional.of(profile));

        TechnicianProfileResponseDTO response =
                technicianProfileService.getProfileByUsername("tech1");

        assertNotNull(response);
        assertEquals("tech1", response.getUsername());
        assertTrue(response.isAvailable());
    }

    // =========================
    // APPROVE TECHNICIAN
    // =========================

    @Test
    void approveTechnician_success() {

        TechnicianProfile profile = TechnicianProfile.builder()
                .id("t1")
                .userId("u1")
                .approvalStatus(TechnicianProfile.ApprovalStatus.PENDING)
                .build();

        User user = User.builder()
                .id("u1")
                .email("tech@test.com")
                .build(); // firstName NOT required for this test

        when(technicianProfileRepository.findById("t1"))
                .thenReturn(Optional.of(profile));

        when(userRepository.findById("u1"))
                .thenReturn(Optional.of(user));

        technicianProfileService.approveTechnician("t1");

        assertEquals(
                TechnicianProfile.ApprovalStatus.APPROVED,
                profile.getApprovalStatus()
        );

        verify(technicianProfileRepository).save(profile);

        //  Correct verification (matches real service behavior)
        verify(emailService)
                .sendWelcomeEmail(eq("tech@test.com"), nullable(String.class));
    }


    // =========================
    // REJECT TECHNICIAN
    // =========================
    @Test
    void rejectTechnician_success() {

        TechnicianProfile profile = TechnicianProfile.builder()
                .id("t1")
                .approvalStatus(TechnicianProfile.ApprovalStatus.PENDING)
                .build();

        when(technicianProfileRepository.findById("t1"))
                .thenReturn(Optional.of(profile));

        technicianProfileService.rejectTechnician("t1", "Invalid documents");

        assertEquals(
                TechnicianProfile.ApprovalStatus.REJECTED,
                profile.getApprovalStatus()
        );

        assertEquals("Invalid documents", profile.getRejectionReason());
        verify(technicianProfileRepository).save(profile);
    }

    // =========================
    // UPDATE AVAILABILITY
    // =========================
    @Test
    void updateAvailability_success() {

        User user = User.builder()
                .id("u1")
                .username("tech1")
                .build();

        TechnicianProfile profile = TechnicianProfile.builder()
                .userId("u1")
                .approvalStatus(TechnicianProfile.ApprovalStatus.APPROVED)
                .available(false)
                .build();

        when(userRepository.findByUsername("tech1"))
                .thenReturn(Optional.of(user));

        when(technicianProfileRepository.findByUserId("u1"))
                .thenReturn(Optional.of(profile));

        technicianProfileService.updateAvailability("tech1", true);

        assertTrue(profile.isAvailable());
        verify(technicianProfileRepository).save(profile);
    }

    // =========================
    // TECHNICIAN NOT FOUND
    // =========================
    @Test
    void approveTechnician_notFound() {

        when(technicianProfileRepository.findById("missing"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> technicianProfileService.approveTechnician("missing")
        );
    }
}
