// Service layer for Hello API business logic
package com.example.aks.service;

import com.example.aks.dto.HelloResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class HelloService {

    @Value("${app.version}")
    private String appVersion;

    @CircuitBreaker(name = "externalService", fallbackMethod = "getFallbackHelloMessage")
    @Retry(name = "externalService")
    public HelloResponse getHelloMessage(String username) {
        log.debug("Generating hello message for user: {}", username);

        // Simulate potential external service call
        // In real scenario, this might call another service or database
        return HelloResponse.create(username, appVersion);
    }

    /**
     * Fallback method for circuit breaker
     */
    public HelloResponse getFallbackHelloMessage(String username, Exception ex) {
        log.warn("Using fallback for user: {} due to: {}", username, ex.getMessage());

        return new HelloResponse(
            "Hello, " + username + "! (Fallback response)",
            username,
            java.time.LocalDateTime.now(),
            appVersion + "-fallback"
        );
    }
}
