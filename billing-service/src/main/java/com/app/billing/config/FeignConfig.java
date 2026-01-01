package com.app.billing.config;

import feign.Logger;
import feign.RequestInterceptor;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework. context.annotation.Bean;
import org.springframework.context.annotation. Configuration;

@Slf4j
@Configuration
public class FeignConfig {

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level. BASIC;
    }

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            requestTemplate.header("X-Internal-Service", "billing-service");
            requestTemplate.header("X-Request-Source", "feign-client");

            log.debug("Feign Request: {} {}",
                    requestTemplate.method(),
                    requestTemplate. url());
        };
    }

    @Bean
    public Retryer retryer() {
        return new Retryer.Default(100, 1000, 3);
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return new CustomErrorDecoder();
    }

    public static class CustomErrorDecoder implements ErrorDecoder {

        private final ErrorDecoder defaultDecoder = new Default();

        @Override
        public Exception decode(String methodKey, feign.Response response) {
            log.error("Feign Error - Method: {}, Status: {}, Reason: {}",
                    methodKey, response.status(), response.reason());

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