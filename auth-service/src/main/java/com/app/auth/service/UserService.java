package com.app.auth.service;

import com.app.auth.model.User;

import java.util.Optional;

public interface UserService {

    User createUser(User user);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
