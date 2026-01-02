package com.app.auth. service;

import com.app.auth. dto.request. ChangePasswordRequest;
import com.app. auth.dto.request.UpdateProfileRequest;
import com.app.auth.dto. response.UserProfileResponseDTO;
import com.app.auth. model.User;

import java.util.Optional;

public interface UserService {

    // Existing methods
    User createUser(User user);

    Optional<User> findById(String id);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // New methods for UserController
    UserProfileResponseDTO getProfileByUsername(String username);

    UserProfileResponseDTO updateProfile(String username, UpdateProfileRequest request);

    void changePassword(String username, ChangePasswordRequest request);

    void deactivateUser(String userId);
}