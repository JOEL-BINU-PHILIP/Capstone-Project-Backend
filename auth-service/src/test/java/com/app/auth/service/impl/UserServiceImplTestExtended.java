package com.app.auth.service.impl;

import com.app.auth.dto.request.ChangePasswordRequest;
import com.app.auth.dto.request.UpdateProfileRequest;
import com.app.auth.dto.response.UserProfileResponseDTO;
import com.app.auth.exception.InvalidCredentialsException;
import com.app.auth.exception.ResourceNotFoundException;
import com.app.auth.model.TechnicianProfile;
import com.app.auth.model.User;
import com.app.auth.model.UserRole;
import com.app.auth.repository.TechnicianProfileRepository;
import com.app.auth.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTestExtended {

    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TechnicianProfileRepository technicianProfileRepository;

    private User testUser;
    private TechnicianProfile testTechnicianProfile;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("user123")
                .username("testuser")
                .email("test@test.com")
                .password("hashedPassword")
                .firstName("Test")
                .lastName("User")
                .phoneNumber("1234567890")
                .city("TestCity")
                .state("TestState")
                .zipCode("12345")
                .roles(Set.of(UserRole.ROLE_CUSTOMER))
                .enabled(true)
                .emailVerified(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        testTechnicianProfile = TechnicianProfile.builder()
                .id("tech123")
                .userId("user123")
                .skills(Set.of("PLUMBING"))
                .experienceYears(5)
                .bio("Experienced plumber")
                .city("TestCity")
                .state("TestState")
                .approvalStatus(TechnicianProfile.ApprovalStatus.APPROVED)
                .available(true)
                .averageRating(4.5)
                .totalJobsCompleted(50)
                .currentActiveJobs(2)
                .createdAt(Instant.now())
                .build();
    }

    // ==================== CREATE USER TESTS ====================

    @Test
    void createUser_success() {
        User newUser = User.builder()
                .username("newuser")
                .email("new@test.com")
                .password("password")
                .build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        User result = userService.createUser(newUser);

        assertNotNull(result);
        verify(passwordEncoder).encode("password");
        verify(userRepository).save(newUser);
    }

    @Test
    void createUser_usernameExists() {
        User newUser = User.builder()
                .username("existinguser")
                .email("new@test.com")
                .password("password")
                .build();

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> userService.createUser(newUser));
    }

    @Test
    void createUser_emailExists() {
        User newUser = User.builder()
                .username("newuser")
                .email("existing@test.com")
                .password("password")
                .build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@test.com")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> userService.createUser(newUser));
    }

    // ==================== FIND USER TESTS ====================

    @Test
    void findByUsername_success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        Optional<User> result = userService.findByUsername("testuser");

        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
    }

    @Test
    void findByUsername_notFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        Optional<User> result = userService.findByUsername("unknown");

        assertFalse(result.isPresent());
    }

    @Test
    void findByEmail_success() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(testUser));

        Optional<User> result = userService.findByEmail("test@test.com");

        assertTrue(result.isPresent());
        assertEquals("test@test.com", result.get().getEmail());
    }

    @Test
    void findById_success() {
        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));

        Optional<User> result = userService.findById("user123");

        assertTrue(result.isPresent());
        assertEquals("user123", result.get().getId());
    }

    @Test
    void existsByUsername_true() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertTrue(userService.existsByUsername("testuser"));
    }

    @Test
    void existsByUsername_false() {
        when(userRepository.existsByUsername("unknown")).thenReturn(false);

        assertFalse(userService.existsByUsername("unknown"));
    }

    @Test
    void existsByEmail_true() {
        when(userRepository.existsByEmail("test@test.com")).thenReturn(true);

        assertTrue(userService.existsByEmail("test@test.com"));
    }

    @Test
    void existsByEmail_false() {
        when(userRepository.existsByEmail("unknown@test.com")).thenReturn(false);

        assertFalse(userService.existsByEmail("unknown@test.com"));
    }

    // ==================== GET PROFILE TESTS ====================

    @Test
    void getProfileByUsername_success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        UserProfileResponseDTO result = userService.getProfileByUsername("testuser");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("Test User", result.getFullName());
    }

    @Test
    void getProfileByUsername_notFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getProfileByUsername("unknown"));
    }

    @Test
    void getProfileByUsername_technicianWithProfile() {
        User techUser = User.builder()
                .id("tech123")
                .username("techuser")
                .email("tech@test.com")
                .firstName("Tech")
                .lastName("User")
                .roles(Set.of(UserRole.ROLE_TECHNICIAN))
                .enabled(true)
                .emailVerified(true)
                .build();

        when(userRepository.findByUsername("techuser")).thenReturn(Optional.of(techUser));
        when(technicianProfileRepository.findByUserId("tech123")).thenReturn(Optional.of(testTechnicianProfile));

        UserProfileResponseDTO result = userService.getProfileByUsername("techuser");

        assertNotNull(result);
        assertNotNull(result.getTechnicianInfo());
        assertEquals("tech123", result.getTechnicianInfo().getTechnicianId());
    }

    @Test
    void getProfileByUsername_withNullNames() {
        User userWithNullNames = User.builder()
                .id("user456")
                .username("nullnameuser")
                .email("null@test.com")
                .firstName(null)
                .lastName(null)
                .roles(Set.of(UserRole.ROLE_CUSTOMER))
                .enabled(true)
                .build();

        when(userRepository.findByUsername("nullnameuser")).thenReturn(Optional.of(userWithNullNames));

        UserProfileResponseDTO result = userService.getProfileByUsername("nullnameuser");

        assertNotNull(result);
        assertEquals("nullnameuser", result.getFullName());
    }

    // ==================== UPDATE PROFILE TESTS ====================

    @Test
    void updateProfile_success() {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .firstName("Updated")
                .lastName("Name")
                .phoneNumber("9876543210")
                .city("NewCity")
                .state("NewState")
                .zipCode("54321")
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfileResponseDTO result = userService.updateProfile("testuser", request);

        assertNotNull(result);
        assertEquals("Updated", result.getFirstName());
        assertEquals("NewCity", result.getCity());
    }

    @Test
    void updateProfile_partialUpdate() {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .firstName("OnlyFirst")
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfileResponseDTO result = userService.updateProfile("testuser", request);

        assertNotNull(result);
        assertEquals("OnlyFirst", result.getFirstName());
    }

    @Test
    void updateProfile_notFound() {
        UpdateProfileRequest request = UpdateProfileRequest.builder().firstName("Test").build();
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.updateProfile("unknown", request));
    }

    @Test
    void updateProfile_technicianWithBio() {
        User techUser = User.builder()
                .id("tech123")
                .username("techuser")
                .roles(Set.of(UserRole.ROLE_TECHNICIAN))
                .build();

        testTechnicianProfile.setApprovalStatus(TechnicianProfile.ApprovalStatus.APPROVED);

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .bio("Updated bio")
                .available(false)
                .build();

        when(userRepository.findByUsername("techuser")).thenReturn(Optional.of(techUser));
        when(userRepository.save(any(User.class))).thenReturn(techUser);
        when(technicianProfileRepository.findByUserId("tech123")).thenReturn(Optional.of(testTechnicianProfile));

        userService.updateProfile("techuser", request);

        verify(technicianProfileRepository).save(any(TechnicianProfile.class));
    }

    @Test
    void updateProfile_technicianNotApproved_availabilityNotUpdated() {
        User techUser = User.builder()
                .id("tech123")
                .username("techuser")
                .roles(Set.of(UserRole.ROLE_TECHNICIAN))
                .build();

        TechnicianProfile pendingProfile = TechnicianProfile.builder()
                .id("tech123")
                .userId("tech123")
                .approvalStatus(TechnicianProfile.ApprovalStatus.PENDING)
                .available(false)  // Start with false
                .build();

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .available(true)  // Try to set to true
                .build();

        when(userRepository.findByUsername("techuser")).thenReturn(Optional.of(techUser));
        when(userRepository.save(any(User.class))).thenReturn(techUser);
        when(technicianProfileRepository.findByUserId("tech123")).thenReturn(Optional.of(pendingProfile));

        userService.updateProfile("techuser", request);

        // Available should not be updated since profile is not approved
        assertFalse(pendingProfile.isAvailable());
    }

    // ==================== CHANGE PASSWORD TESTS ====================

    @Test
    void changePassword_success() {
        ChangePasswordRequest request = new ChangePasswordRequest("oldPassword", "newPassword", "newPassword");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", "hashedPassword")).thenReturn(true);
        when(passwordEncoder.matches("newPassword", "hashedPassword")).thenReturn(false);
        when(passwordEncoder.encode("newPassword")).thenReturn("newHashedPassword");

        userService.changePassword("testuser", request);

        verify(userRepository).save(testUser);
        assertEquals("newHashedPassword", testUser.getPassword());
    }

    @Test
    void changePassword_wrongCurrentPassword() {
        ChangePasswordRequest request = new ChangePasswordRequest("wrongPassword", "newPassword", "newPassword");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", "hashedPassword")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> userService.changePassword("testuser", request));
    }

    @Test
    void changePassword_passwordMismatch() {
        ChangePasswordRequest request = new ChangePasswordRequest("oldPassword", "newPassword", "differentPassword");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", "hashedPassword")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> userService.changePassword("testuser", request));
    }

    @Test
    void changePassword_sameAsOldPassword() {
        ChangePasswordRequest request = new ChangePasswordRequest("oldPassword", "oldPassword", "oldPassword");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", "hashedPassword")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> userService.changePassword("testuser", request));
    }

    @Test
    void changePassword_userNotFound() {
        ChangePasswordRequest request = new ChangePasswordRequest("old", "new", "new");
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.changePassword("unknown", request));
    }

    // ==================== DEACTIVATE USER TESTS ====================

    @Test
    void deactivateUser_success() {
        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));

        userService.deactivateUser("user123");

        assertFalse(testUser.isEnabled());
        verify(userRepository).save(testUser);
    }

    @Test
    void deactivateUser_notFound() {
        when(userRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.deactivateUser("unknown"));
    }
}

