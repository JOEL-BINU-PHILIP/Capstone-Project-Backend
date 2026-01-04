package com.app.auth.service.impl;

import com.app.auth.dto.response.TechnicianProfileResponseDTO;
import com.app.auth.exception.ResourceNotFoundException;
import com.app.auth.model.TechnicianProfile;
import com.app.auth.model.User;
import com.app.auth.repository.TechnicianProfileRepository;
import com.app.auth.repository.UserRepository;
import com.app.auth.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TechnicianProfileServiceImplTest {

    @Mock private TechnicianProfileRepository technicianProfileRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;

    @InjectMocks
    private TechnicianProfileServiceImpl technicianProfileService;

    private TechnicianProfile profile;
    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id("user-1").username("tech_user").email("tech@test.com").build();
        profile = TechnicianProfile.builder()
                .id("tech-profile-1")
                .userId("user-1")
                .approvalStatus(TechnicianProfile.ApprovalStatus.PENDING)
                .available(false)
                .build();
    }

    @Test
    void createProfile_Success() {
        when(technicianProfileRepository.existsByUserId("user-1")).thenReturn(false);
        when(technicianProfileRepository.save(any(TechnicianProfile.class))).thenReturn(profile);

        TechnicianProfile created = technicianProfileService.createProfile(
                "user-1", Set.of("Skill"), 5, "Bio", "City", "State", "AADHAAR"
        );

        assertNotNull(created);
        assertEquals(TechnicianProfile.ApprovalStatus.PENDING, created.getApprovalStatus());
        verify(technicianProfileRepository).save(any(TechnicianProfile.class));
    }

    @Test
    void approveTechnician_Success() {
        when(technicianProfileRepository.findById("tech-profile-1")).thenReturn(Optional.of(profile));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        technicianProfileService.approveTechnician("tech-profile-1");

        assertEquals(TechnicianProfile.ApprovalStatus.APPROVED, profile.getApprovalStatus());
        assertTrue(profile.isAvailable()); // Should become available upon approval
        assertNotNull(profile.getApprovedAt());

        verify(technicianProfileRepository).save(profile);
        verify(emailService).sendWelcomeEmail("tech@test.com", "tech_user");
    }

    @Test
    void rejectTechnician_Success() {
        when(technicianProfileRepository.findById("tech-profile-1")).thenReturn(Optional.of(profile));

        technicianProfileService.rejectTechnician("tech-profile-1", "Invalid Documents");

        assertEquals(TechnicianProfile.ApprovalStatus.REJECTED, profile.getApprovalStatus());
        assertEquals("Invalid Documents", profile.getRejectionReason());

        verify(technicianProfileRepository).save(profile);
    }

    @Test
    void approveTechnician_AlreadyApproved_ThrowsException() {
        profile.setApprovalStatus(TechnicianProfile.ApprovalStatus.APPROVED);
        when(technicianProfileRepository.findById("tech-profile-1")).thenReturn(Optional.of(profile));

        assertThrows(IllegalStateException.class, () ->
                technicianProfileService.approveTechnician("tech-profile-1")
        );
    }
    // ... inside TechnicianProfileServiceImplTest class ...

    @Test
    void getTechniciansByStatus_Success() {
        // Given
        PageRequest pageable = PageRequest.of(0, 10);
        List<TechnicianProfile> profiles = List.of(profile);
        Page<TechnicianProfile> profilePage = new PageImpl<>(profiles);

        when(technicianProfileRepository.findByApprovalStatusAndAvailable(
                eq(TechnicianProfile.ApprovalStatus.APPROVED),
                eq(true),
                eq(pageable))
        ).thenReturn(profilePage);

        when(userRepository.findById(anyString())).thenReturn(Optional.of(user));

        // When
        Page<TechnicianProfileResponseDTO> result = technicianProfileService.getTechniciansByStatus(
                TechnicianProfile.ApprovalStatus.APPROVED,
                true,
                pageable
        );

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("tech_user", result.getContent().get(0).getUsername());
        verify(technicianProfileRepository).findByApprovalStatusAndAvailable(any(), anyBoolean(), any());
    }

    @Test
    void getProfileById_Success() {
        when(technicianProfileRepository.findById("tech-profile-1")).thenReturn(Optional.of(profile));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        TechnicianProfileResponseDTO result = technicianProfileService.getProfileById("tech-profile-1");

        assertNotNull(result);
        assertEquals("tech-profile-1", result.getId());
        assertEquals("tech_user", result.getUsername());
    }

    @Test
    void getProfileById_NotFound_ThrowsException() {
        when(technicianProfileRepository.findById("invalid-id")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                technicianProfileService.getProfileById("invalid-id")
        );
    }
}