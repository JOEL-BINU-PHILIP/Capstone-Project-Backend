package com.app.gateway. controller;

import lombok.extern. slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org. springframework.web.bind.annotation. GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher. Mono;

import java.time. Instant;
import java.util. HashMap;
import java.util. Map;

@Slf4j
@RestController
public class HealthCheckController {

    private final WebClient webClient;

    public HealthCheckController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> root() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "API Gateway");
        response.put("status", "UP");
        response.put("timestamp", Instant.now().toString());
        response.put("message", "Welcome to Service Management System API Gateway");

        Map<String, String> endpoints = new HashMap<>();
        endpoints. put("auth", "/api/auth/**");
        endpoints.put("catalog", "/api/services/**");
        endpoints.put("booking", "/api/bookings/**");
        endpoints.put("billing", "/api/billing/**");
        endpoints.put("notifications", "/api/notifications/**");
        response.put("endpoints", endpoints);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "API Gateway");
        response.put("timestamp", Instant. now().toString());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/health/services")
    public Mono<ResponseEntity<Map<String, Object>>> servicesHealth() {
        Map<String, Object> response = new HashMap<>();
        response.put("gateway", "UP");
        response.put("timestamp", Instant.now().toString());

        // Check each service health
        return Mono.zip(
                checkServiceHealth("http://localhost:8081/actuator/health", "auth-service"),
                checkServiceHealth("http://localhost:8082/actuator/health", "catalog-service"),
                checkServiceHealth("http://localhost:8083/actuator/health", "booking-service"),
                checkServiceHealth("http://localhost:8084/actuator/health", "billing-service"),
                checkServiceHealth("http://localhost:8085/actuator/health", "notification-service")
        ).map(tuple -> {
            Map<String, Object> services = new HashMap<>();
            services. put("auth-service", tuple.getT1());
            services.put("catalog-service", tuple.getT2());
            services.put("booking-service", tuple.getT3());
            services.put("billing-service", tuple.getT4());
            services.put("notification-service", tuple.getT5());
            response.put("services", services);
            return ResponseEntity.ok(response);
        }).onErrorReturn(ResponseEntity.ok(response));
    }

    private Mono<String> checkServiceHealth(String url, String serviceName) {
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .map(body -> "UP")
                .onErrorReturn("DOWN")
                .timeout(java.time.Duration.ofSeconds(3))
                .onErrorReturn("DOWN");
    }
}