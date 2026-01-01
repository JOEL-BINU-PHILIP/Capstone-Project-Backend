package com.app.booking.config;

import feign.Logger;
import feign.RequestInterceptor;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework. context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class FeignConfig {

    /**
     * Feign Logger Level
     * NONE - No logging
     * BASIC - Log request method and URL, response status code and execution time
     * HEADERS - Log basic + request and response headers
     * FULL - Log everything
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    /**
     * Request Interceptor - Add any headers needed for internal service calls
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            // Add internal service header for identification
            requestTemplate.header("X-Internal-Service", "booking-service");
            requestTemplate.header("X-Request-Source", "feign-client");

            log.debug("Feign Request: {} {}",
                    requestTemplate.method(),
                    requestTemplate.url());
        };
    }

    /**
     * Retry configuration
     * period - Initial interval in milliseconds
     * maxPeriod - Maximum interval in milliseconds
     * maxAttempts - Maximum number of attempts
     */
    @Bean
    public Retryer retryer() {
        return new Retryer.Default(
                100,    // Initial interval:  100ms
                1000,   // Max interval: 1 second
                3       // Max attempts: 3
        );
    }

    /**
     * Custom Error Decoder
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return new CustomErrorDecoder();
    }

    /**
     * Custom Error Decoder class
     */
    public static class CustomErrorDecoder implements ErrorDecoder {

        private final ErrorDecoder defaultDecoder = new Default();

        @Override
        public Exception decode(String methodKey, feign.Response response) {
            log.error("Feign Error - Method: {}, Status: {}, Reason: {}",
                    methodKey,
                    response.status(),
                    response.reason());

            // Handle specific status codes
            switch (response.status()) {
                case 404:
                    return new ServiceNotFoundException("Resource not found:  " + methodKey);
                case 503:
                    return new ServiceUnavailableException("Service unavailable: " + methodKey);
                default:
                    return defaultDecoder.decode(methodKey, response);
            }
        }
    }

    // Custom Exceptions
    public static class ServiceNotFoundException extends RuntimeException {
        public ServiceNotFoundException(String message) {
            super(message);
        }
    }

    public static class ServiceUnavailableException extends RuntimeException {
        public ServiceUnavailableException(String message) {
            super(message);
        }
    }
}