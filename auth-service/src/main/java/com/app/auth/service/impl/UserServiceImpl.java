package com. app.auth.service.impl;

import com.app.auth.dto.request. ChangePasswordRequest;
import com.app.auth.dto.request. UpdateProfileRequest;
import com.app.auth.dto.response. UserProfileResponseDTO;
import com. app.auth.exception.InvalidCredentialsException;
import com.app.auth.exception.ResourceNotFoundException;
import com.app.auth. model.TechnicianProfile;
import com.app.auth.model.User;
import com. app.auth.model.UserRole;
import com.app. auth.repository.TechnicianProfileRepository;
import com. app.auth.repository.UserRepository;
import com.app. auth.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework. transaction.annotation. Transactional;

import java.time. Instant;
import java.util.Optional;
import java.util. stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TechnicianProfileRepository technicianProfileRepository;

    // ========== EXISTING METHODS ==========

    @Override
    public User createUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(user. getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }

    // ========== NEW METHODS ==========

    @Override
    public UserProfileResponseDTO getProfileByUsername(String username) {
        User user = userRepository. findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found:  " + username));

        return mapToProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponseDTO updateProfile(String username, UpdateProfileRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found:  " + username));

        // Update basic profile fields
        if (request.getFirstName() != null) {
            user. setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request. getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getCity() != null) {
            user.setCity(request.getCity());
        }
        if (request.getState() != null) {
            user.setState(request.getState());
        }
        if (request. getZipCode() != null) {
            user.setZipCode(request. getZipCode());
        }

        user.setUpdatedAt(Instant.now());
        User savedUser = userRepository. save(user);

        // If user is a technician, update technician-specific fields
        if (user.getRoles().contains(UserRole.ROLE_TECHNICIAN)) {
            Optional<TechnicianProfile> profileOpt = technicianProfileRepository.findByUserId(user. getId());
            if (profileOpt. isPresent()) {
                TechnicianProfile profile = profileOpt.get();
                boolean updated = false;

                if (request.getBio() != null) {
                    profile. setBio(request. getBio());
                    updated = true;
                }
                if (request.getAvailable() != null) {
                    // Only update availability if technician is approved
                    if (profile.getApprovalStatus() == TechnicianProfile.ApprovalStatus. APPROVED) {
                        profile.setAvailable(request.getAvailable());
                        updated = true;
                    }
                }

                if (updated) {
                    profile.setUpdatedAt(Instant.now());
                    technicianProfileRepository. save(profile);
                }
            }
        }

        log.info("Profile updated for user: {}", username);
        return mapToProfileResponse(savedUser);
    }

    @Override
    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        // Validate current password
        if (!passwordEncoder. matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        // Validate new password matches confirmation
        if (! request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirmation do not match");
        }

        // Validate new password is different from current
        if (passwordEncoder.matches(request. getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("New password must be different from current password");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        log.info("Password changed for user: {}", username);
    }

    @Override
    @Transactional
    public void deactivateUser(String userId) {
        User user = userRepository. findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        user.setEnabled(false);
        user.setUpdatedAt(Instant. now());
        userRepository.save(user);

        log.info("User deactivated: {}", userId);
    }

    // ========== HELPER METHODS ==========

    private UserProfileResponseDTO mapToProfileResponse(User user) {
        UserProfileResponseDTO. UserProfileResponseDTOBuilder builder = UserProfileResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(getFullName(user))
                .phoneNumber(user.getPhoneNumber())
                .city(user. getCity())
                .state(user. getState())
                .zipCode(user.getZipCode())
                .roles(user.getRoles().stream()
                        .map(UserRole::name)
                        . collect(Collectors. toList()))
                .enabled(user.isEnabled())
                .emailVerified(user. isEmailVerified())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLoginAt(user.getLastLoginAt());

        // Add technician info if user is a technician
        if (user.getRoles().contains(UserRole. ROLE_TECHNICIAN)) {
            Optional<TechnicianProfile> profileOpt = technicianProfileRepository.findByUserId(user.getId());
            if (profileOpt.isPresent()) {
                TechnicianProfile profile = profileOpt.get();
                builder. technicianInfo(UserProfileResponseDTO.TechnicianInfo.builder()
                        .technicianId(profile.getId())
                        . skills(profile.getSkills())
                        .experienceYears(profile.getExperienceYears())
                        . bio(profile.getBio())
                        . approvalStatus(profile. getApprovalStatus().name())
                        .available(profile.isAvailable())
                        .averageRating(profile.getAverageRating())
                        .totalJobsCompleted(profile.getTotalJobsCompleted())
                        . currentActiveJobs(profile. getCurrentActiveJobs())
                        .approvedAt(profile.getApprovedAt())
                        .rejectionReason(profile. getRejectionReason())
                        . build());
            }
        }

        return builder.build();
    }

    private String getFullName(User user) {
        String firstName = user.getFirstName() != null ? user. getFirstName() : "";
        String lastName = user.getLastName() != null ? user.getLastName() : "";
        String fullName = (firstName + " " + lastName).trim();
        return fullName. isEmpty() ? user.getUsername() : fullName;
    }
}