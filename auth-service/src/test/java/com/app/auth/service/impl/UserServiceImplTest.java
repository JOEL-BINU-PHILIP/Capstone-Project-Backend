package com.app.auth.service.impl;

import com.app.auth.dto.request.ChangePasswordRequest;
import com.app.auth.dto.request.UpdateProfileRequest;
import com.app.auth.dto.response.UserProfileResponseDTO;
import com.app.auth.exception.InvalidCredentialsException;
import com.app.auth.model.User;
import com.app.auth.repository.TechnicianProfileRepository;
import com.app.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TechnicianProfileRepository technicianProfileRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id("1")
                .username("testuser")
                .firstName("OldName")
                .password("encodedOldPass")
                .build();
    }

    @Test
    void getProfileByUsername_Success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        UserProfileResponseDTO profile = userService.getProfileByUsername("testuser");

        assertNotNull(profile);
        assertEquals("testuser", profile.getUsername());
        assertEquals("OldName", profile.getFirstName());
    }

    @Test
    void updateProfile_Success() {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .firstName("NewName")
                .city("NewCity")
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponseDTO updated = userService.updateProfile("testuser", request);

        assertEquals("NewName", updated.getFirstName());
        assertEquals("NewCity", updated.getCity());
    }

    @Test
    void changePassword_Success() {
        ChangePasswordRequest request = new ChangePasswordRequest("oldPass", "newPass123", "newPass123");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPass", "encodedOldPass")).thenReturn(true);
        when(passwordEncoder.encode("newPass123")).thenReturn("encodedNewPass");

        userService.changePassword("testuser", request);

        verify(userRepository).save(user);
        assertEquals("encodedNewPass", user.getPassword());
    }

    @Test
    void changePassword_WrongCurrentPassword() {
        ChangePasswordRequest request = new ChangePasswordRequest("wrongPass", "newPass", "newPass");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPass", "encodedOldPass")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> userService.changePassword("testuser", request));
    }
}