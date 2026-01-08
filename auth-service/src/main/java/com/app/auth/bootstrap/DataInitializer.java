package com.app.auth.bootstrap;

import com.app.auth.model.User;
import com.app.auth.model.UserRole;
import com.app.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner initDefaultUsers() {
        return args -> {

            // ======================
            // ADMIN
            // ======================
            if (!userRepository.existsByUsername("admin")) {
                User admin = User.builder()
                        .username("admin")
                        .email("admin@system.com")
                        .password(passwordEncoder.encode("Admin@123"))
                        .roles(Set.of(UserRole.valueOf("ROLE_ADMIN")))
                        .enabled(true)
                        .accountNonLocked(true)
                        .accountNonExpired(true)
                        .credentialsNonExpired(true)
                        .emailVerified(true)
                        .build();

                userRepository.save(admin);
                System.out.println("Admin user created");
            }

            // ======================
            // SERVICE MANAGER
            // ======================
            if (!userRepository.existsByUsername("service_manager")) {
                User serviceManager = User.builder()
                        .username("service_manager")
                        .email("manager@system.com")
                        .password(passwordEncoder.encode("Manager@123"))
                        .roles(Set.of(UserRole.valueOf("ROLE_SERVICE_MANAGER")))
                        .enabled(true)
                        .accountNonLocked(true)
                        .accountNonExpired(true)
                        .credentialsNonExpired(true)
                        .emailVerified(true)
                        .build();

                userRepository.save(serviceManager);
                System.out.println("Service Manager user created");
            }
        };
    }
}
