package com.app.auth.service.impl;

import com.app.auth.dto.request.ChangePasswordRequest;
import com.app.auth.dto.request.UpdateProfileRequest;
import com.app.auth.dto.response.UserProfileResponseDTO;
import com.app.auth.exception.InvalidCredentialsException;
import com.app.auth.exception.ResourceNotFoundException;
import com.app.auth.model.User;
import com.app.auth.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    // =========================
    // GET PROFILE
    // =========================
    @Test
    void getProfile_success() {

        User user = User.builder()
                .id("u1")
                .username("john")
                .email("john@test.com")
                .firstName("John")
                .lastName("Doe")
                .enabled(true)
                .emailVerified(true)
                .roles(Set.of())
                .build();

        when(userRepository.findByUsername("john"))
                .thenReturn(Optional.of(user));

        UserProfileResponseDTO response =
                userService.getProfileByUsername("john");

        assertNotNull(response);
        assertEquals("john", response.getUsername());
        assertEquals("John", response.getFirstName());
    }

    // =========================
    // UPDATE PROFILE
    // =========================
    @Test
    void updateProfile_success() {

        User user = User.builder()
                .username("john")
                .firstName("Old")
                .lastName("Name")
                .build();

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .firstName("New")
                .city("Bangalore")
                .build();

        when(userRepository.findByUsername("john"))
                .thenReturn(Optional.of(user));

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponseDTO response =
                userService.updateProfile("john", request);

        assertEquals("New", response.getFirstName());
        assertEquals("Bangalore", response.getCity());
    }

    // =========================
    // CHANGE PASSWORD – SUCCESS
    // =========================
    @Test
    void changePassword_success() {

        User user = User.builder()
                .username("john")
                .password("hashed-old")
                .build();

        when(userRepository.findByUsername("john"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("Old@123", "hashed-old"))
                .thenReturn(true);

        when(passwordEncoder.encode("New@1234"))
                .thenReturn("hashed-new");

        ChangePasswordRequest request =
                new ChangePasswordRequest("Old@123", "New@1234", "New@1234");

        userService.changePassword("john", request);

        verify(userRepository).save(any(User.class));
        assertEquals("hashed-new", user.getPassword());
    }

    // =========================
    // CHANGE PASSWORD – WRONG CURRENT PASSWORD
    // =========================
    @Test
    void changePassword_fails_whenCurrentPasswordWrong() {

        User user = User.builder()
                .username("john")
                .password("hashed-old")
                .build();

        when(userRepository.findByUsername("john"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(any(), any()))
                .thenReturn(false);

        ChangePasswordRequest request =
                new ChangePasswordRequest("Wrong@123", "New@1234", "New@1234");

        assertThrows(
                InvalidCredentialsException.class,
                () -> userService.changePassword("john", request)
        );
    }

    // =========================
    // DEACTIVATE USER
    // =========================
    @Test
    void deactivateUser_success() {

        User user = User.builder()
                .enabled(true)
                .build();

        when(userRepository.findById("u1"))
                .thenReturn(Optional.of(user));

        userService.deactivateUser("u1");

        assertFalse(user.isEnabled());
        verify(userRepository).save(user);
    }

    // =========================
    // USER NOT FOUND
    // =========================
    @Test
    void getProfile_userNotFound() {

        when(userRepository.findByUsername("missing"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> userService.getProfileByUsername("missing")
        );
    }
}
