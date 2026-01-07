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
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TechnicianProfileServiceImplTestExtended {

    @InjectMocks
    private TechnicianProfileServiceImpl technicianProfileService;

    @Mock
    private TechnicianProfileRepository technicianProfileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    private User testUser;
    private TechnicianProfile testProfile;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("user123")
                .username("techuser")
                .email("tech@test.com")
                .firstName("Tech")
                .lastName("User")
                .phoneNumber("1234567890")
                .build();

        testProfile = TechnicianProfile.builder()
                .id("tech123")
                .userId("user123")
                .skills(Set.of("PLUMBING", "ELECTRICAL"))
                .experienceYears(5)
                .bio("Experienced technician")
                .city("TestCity")
                .state("TestState")
                .idProofType("AADHAAR")
                .approvalStatus(TechnicianProfile.ApprovalStatus.APPROVED)
                .available(true)
                .averageRating(4.5)
                .totalJobsCompleted(50)
                .currentActiveJobs(2)
                .createdAt(Instant.now())
                .build();
    }

    // ==================== CREATE PROFILE TESTS ====================

    @Test
    void createProfile_success() {
        when(technicianProfileRepository.existsByUserId("newUser123")).thenReturn(false);
        when(technicianProfileRepository.save(any(TechnicianProfile.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TechnicianProfile result = technicianProfileService.createProfile(
                "newUser123",
                Set.of("PLUMBING"),
                5,
                "Experienced plumber",
                "TestCity",
                "TestState",
                "AADHAAR"
        );

        assertNotNull(result);
        assertEquals("newUser123", result.getUserId());
        assertEquals(TechnicianProfile.ApprovalStatus.PENDING, result.getApprovalStatus());
        assertFalse(result.isAvailable()); // Not available until approved
        verify(technicianProfileRepository).save(any(TechnicianProfile.class));
    }

    @Test
    void createProfile_alreadyExists() {
        when(technicianProfileRepository.existsByUserId("user123")).thenReturn(true);

        assertThrows(IllegalStateException.class, () ->
                technicianProfileService.createProfile(
                        "user123", Set.of("PLUMBING"), 5, "Bio", "City", "State", "AADHAAR"
                )
        );
    }

    // ==================== GET PROFILE BY USERNAME TESTS ====================

    @Test
    void getProfileByUsername_success() {
        when(userRepository.findByUsername("techuser")).thenReturn(Optional.of(testUser));
        when(technicianProfileRepository.findByUserId("user123")).thenReturn(Optional.of(testProfile));

        TechnicianProfileResponseDTO result = technicianProfileService.getProfileByUsername("techuser");

        assertNotNull(result);
        assertEquals("techuser", result.getUsername());
        assertEquals("tech@test.com", result.getEmail());
        assertTrue(result.isAvailable());
    }

    @Test
    void getProfileByUsername_userNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                technicianProfileService.getProfileByUsername("unknown")
        );
    }

    @Test
    void getProfileByUsername_profileNotFound() {
        when(userRepository.findByUsername("techuser")).thenReturn(Optional.of(testUser));
        when(technicianProfileRepository.findByUserId("user123")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                technicianProfileService.getProfileByUsername("techuser")
        );
    }

    // ==================== UPDATE AVAILABILITY TESTS ====================

    @Test
    void updateAvailability_success() {
        when(userRepository.findByUsername("techuser")).thenReturn(Optional.of(testUser));
        when(technicianProfileRepository.findByUserId("user123")).thenReturn(Optional.of(testProfile));

        technicianProfileService.updateAvailability("techuser", false);

        assertFalse(testProfile.isAvailable());
        verify(technicianProfileRepository).save(testProfile);
    }

    @Test
    void updateAvailability_notApproved() {
        testProfile.setApprovalStatus(TechnicianProfile.ApprovalStatus.PENDING);

        when(userRepository.findByUsername("techuser")).thenReturn(Optional.of(testUser));
        when(technicianProfileRepository.findByUserId("user123")).thenReturn(Optional.of(testProfile));

        assertThrows(IllegalStateException.class, () ->
                technicianProfileService.updateAvailability("techuser", true)
        );
    }

    @Test
    void updateAvailability_userNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                technicianProfileService.updateAvailability("unknown", true)
        );
    }

    @Test
    void updateAvailability_profileNotFound() {
        when(userRepository.findByUsername("techuser")).thenReturn(Optional.of(testUser));
        when(technicianProfileRepository.findByUserId("user123")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                technicianProfileService.updateAvailability("techuser", true)
        );
    }

    // ==================== APPROVE TECHNICIAN TESTS ====================

    @Test
    void approveTechnician_success() {
        testProfile.setApprovalStatus(TechnicianProfile.ApprovalStatus.PENDING);

        when(technicianProfileRepository.findById("tech123")).thenReturn(Optional.of(testProfile));
        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));

        technicianProfileService.approveTechnician("tech123");

        assertEquals(TechnicianProfile.ApprovalStatus.APPROVED, testProfile.getApprovalStatus());
        assertTrue(testProfile.isAvailable());
        assertNotNull(testProfile.getApprovedAt());
        verify(technicianProfileRepository).save(testProfile);
        verify(emailService).sendWelcomeEmail(eq("tech@test.com"), eq("techuser"));
    }

    @Test
    void approveTechnician_alreadyApproved() {
        testProfile.setApprovalStatus(TechnicianProfile.ApprovalStatus.APPROVED);

        when(technicianProfileRepository.findById("tech123")).thenReturn(Optional.of(testProfile));

        assertThrows(IllegalStateException.class, () ->
                technicianProfileService.approveTechnician("tech123")
        );
    }

    @Test
    void approveTechnician_notFound() {
        when(technicianProfileRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                technicianProfileService.approveTechnician("unknown")
        );
    }

    @Test
    void approveTechnician_userNotFound() {
        testProfile.setApprovalStatus(TechnicianProfile.ApprovalStatus.PENDING);

        when(technicianProfileRepository.findById("tech123")).thenReturn(Optional.of(testProfile));
        when(userRepository.findById("user123")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                technicianProfileService.approveTechnician("tech123")
        );
    }

    // ==================== REJECT TECHNICIAN TESTS ====================

    @Test
    void rejectTechnician_success() {
        testProfile.setApprovalStatus(TechnicianProfile.ApprovalStatus.PENDING);

        when(technicianProfileRepository.findById("tech123")).thenReturn(Optional.of(testProfile));

        technicianProfileService.rejectTechnician("tech123", "Invalid documents");

        assertEquals(TechnicianProfile.ApprovalStatus.REJECTED, testProfile.getApprovalStatus());
        assertEquals("Invalid documents", testProfile.getRejectionReason());
        verify(technicianProfileRepository).save(testProfile);
    }

    @Test
    void rejectTechnician_notFound() {
        when(technicianProfileRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                technicianProfileService.rejectTechnician("unknown", "Reason")
        );
    }

    // ==================== GET TECHNICIANS BY STATUS TESTS ====================

    @Test
    void getTechniciansByStatus_withoutAvailability() {
        Page<TechnicianProfile> profilePage = new PageImpl<>(List.of(testProfile));
        Pageable pageable = PageRequest.of(0, 10);

        when(technicianProfileRepository.findByApprovalStatus(
                TechnicianProfile.ApprovalStatus.APPROVED, pageable)).thenReturn(profilePage);
        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));

        Page<TechnicianProfileResponseDTO> result = technicianProfileService.getTechniciansByStatus(
                TechnicianProfile.ApprovalStatus.APPROVED, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getTechniciansByStatus_withAvailability() {
        Page<TechnicianProfile> profilePage = new PageImpl<>(List.of(testProfile));
        Pageable pageable = PageRequest.of(0, 10);

        when(technicianProfileRepository.findByApprovalStatusAndAvailable(
                TechnicianProfile.ApprovalStatus.APPROVED, true, pageable)).thenReturn(profilePage);
        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));

        Page<TechnicianProfileResponseDTO> result = technicianProfileService.getTechniciansByStatus(
                TechnicianProfile.ApprovalStatus.APPROVED, true, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getTechniciansByStatus_userNotFound_returnsNull() {
        Page<TechnicianProfile> profilePage = new PageImpl<>(List.of(testProfile));
        Pageable pageable = PageRequest.of(0, 10);

        when(technicianProfileRepository.findByApprovalStatus(
                TechnicianProfile.ApprovalStatus.APPROVED, pageable)).thenReturn(profilePage);
        when(userRepository.findById("user123")).thenReturn(Optional.empty());

        Page<TechnicianProfileResponseDTO> result = technicianProfileService.getTechniciansByStatus(
                TechnicianProfile.ApprovalStatus.APPROVED, null, pageable);

        // Should return page with null elements when user not found
        assertNotNull(result);
    }

    // ==================== GET ALL TECHNICIANS TESTS ====================

    @Test
    void getAllTechnicians_success() {
        Page<TechnicianProfile> profilePage = new PageImpl<>(List.of(testProfile));
        Pageable pageable = PageRequest.of(0, 10);

        when(technicianProfileRepository.findAll(pageable)).thenReturn(profilePage);
        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));

        Page<TechnicianProfileResponseDTO> result = technicianProfileService.getAllTechnicians(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getAllTechnicians_emptyPage() {
        Page<TechnicianProfile> emptyPage = new PageImpl<>(List.of());
        Pageable pageable = PageRequest.of(0, 10);

        when(technicianProfileRepository.findAll(pageable)).thenReturn(emptyPage);

        Page<TechnicianProfileResponseDTO> result = technicianProfileService.getAllTechnicians(pageable);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
    }

    // ==================== GET PROFILE BY ID TESTS ====================

    @Test
    void getProfileById_success() {
        when(technicianProfileRepository.findById("tech123")).thenReturn(Optional.of(testProfile));
        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));

        TechnicianProfileResponseDTO result = technicianProfileService.getProfileById("tech123");

        assertNotNull(result);
        assertEquals("tech123", result.getId());
        assertEquals("techuser", result.getUsername());
    }

    @Test
    void getProfileById_profileNotFound() {
        when(technicianProfileRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                technicianProfileService.getProfileById("unknown")
        );
    }

    @Test
    void getProfileById_userNotFound() {
        when(technicianProfileRepository.findById("tech123")).thenReturn(Optional.of(testProfile));
        when(userRepository.findById("user123")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                technicianProfileService.getProfileById("tech123")
        );
    }

    // ==================== EDGE CASES ====================

    @Test
    void mapToDTO_withAllFields() {
        when(technicianProfileRepository.findById("tech123")).thenReturn(Optional.of(testProfile));
        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));

        TechnicianProfileResponseDTO result = technicianProfileService.getProfileById("tech123");

        assertEquals("tech123", result.getId());
        assertEquals("user123", result.getUserId());
        assertEquals("techuser", result.getUsername());
        assertEquals("tech@test.com", result.getEmail());
        assertEquals("Tech", result.getFirstName());
        assertEquals("User", result.getLastName());
        assertEquals("1234567890", result.getPhoneNumber());
        assertEquals(Set.of("PLUMBING", "ELECTRICAL"), result.getSkills());
        assertEquals(5, result.getExperienceYears());
        assertEquals("Experienced technician", result.getBio());
        assertEquals("TestCity", result.getCity());
        assertEquals("TestState", result.getState());
        assertEquals(TechnicianProfile.ApprovalStatus.APPROVED, result.getApprovalStatus());
        assertEquals(4.5, result.getAverageRating());
        assertEquals(50, result.getTotalJobsCompleted());
        assertTrue(result.isAvailable());
    }
}

