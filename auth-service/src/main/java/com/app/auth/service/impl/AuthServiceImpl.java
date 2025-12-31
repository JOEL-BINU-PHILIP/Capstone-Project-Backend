package com.app.auth.service.impl;

import com.app.auth.model.User;
import com.app.auth.service.AuthService;
import com.app.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User register(String username, String email, String password) {

        User user = User.builder()
                .username(username)
                .email(email)
                .password(password)
                .roles(Set.of("ROLE_CUSTOMER"))   // default
                .enabled(true)
                .build();

        return userService.createUser(user);
    }


    @Override
    public User authenticate(String username, String password) {

        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }

        return user;
    }
}
