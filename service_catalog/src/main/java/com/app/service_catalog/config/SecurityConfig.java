package com.app.service_catalog.config;

import com.app.service_catalog. security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation. Configuration;
import org.springframework.security. config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation. web.builders.HttpSecurity;
import org.springframework.security.config.annotation. web.configurers.AbstractHttpConfigurer;
import org.springframework. security.config.http.SessionCreationPolicy;
import org. springframework.security.web.SecurityFilterChain;
import org. springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework. web.cors.CorsConfiguration;
import org.springframework.web.cors. CorsConfigurationSource;
import org. springframework.web.cors. UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util. List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .cors(cors -> cors. configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session. sessionCreationPolicy(SessionCreationPolicy. STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints - Categories and Services (read-only GET requests)
                        . requestMatchers("GET", "/api/services/categories/**").permitAll()
                        .requestMatchers("GET", "/api/services/**").permitAll()

                        // Internal APIs for inter-service communication
                        .requestMatchers("/api/internal/**").permitAll()

                        // Actuator endpoints
                        .requestMatchers("/actuator/**").permitAll()

                        // All other requests need authentication
                        . anyRequest().authenticated()
                )
                // ADD THIS LINE - This is the critical fix!
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:4200",
                "http://localhost:8080",  // API Gateway
                "http://localhost:8081",  // Auth Service
                "http://localhost:8083",  // Booking Service
                "http://localhost:8084",  // Billing Service
                "http://localhost:8085"   // Notification Service
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}