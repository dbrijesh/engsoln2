// REST controller for Hello API endpoints
package com.example.aks.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import com.example.aks.dto.HelloResponse;
import com.example.aks.service.HelloService;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Hello API", description = "Sample endpoints for AKS Starter Kit")
public class HelloController {

  private final HelloService helloService;

  @GetMapping("/hello")
  @RateLimiter(name = "api")
  @Operation(
      summary = "Get hello message",
      description = "Returns a personalized hello message for the authenticated user",
      security = @SecurityRequirement(name = "bearer-jwt"))
  public ResponseEntity<HelloResponse> hello(Authentication authentication) {
    String username = extractUsername(authentication);
    log.info("Hello endpoint called by user: {}", username);

    HelloResponse response = helloService.getHelloMessage(username);

    return ResponseEntity.ok(response);
  }

  @GetMapping("/public/health")
  @Operation(
      summary = "Public health check",
      description = "Public endpoint for health verification")
  public ResponseEntity<String> publicHealth() {
    return ResponseEntity.ok("OK");
  }

  private String extractUsername(Authentication authentication) {
    // Handle local testing without authentication
    if (authentication == null) {
      return "local-user";
    }

    if (authentication.getPrincipal() instanceof Jwt jwt) {
      // Try to get preferred_username or name from Azure AD token
      String preferredUsername = jwt.getClaimAsString("preferred_username");
      if (preferredUsername != null) {
        return preferredUsername;
      }

      String name = jwt.getClaimAsString("name");
      if (name != null) {
        return name;
      }
    }

    return authentication.getName();
  }
}
