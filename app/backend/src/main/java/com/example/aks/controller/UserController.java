// REST controller for User management with validation examples
package com.example.aks.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.aks.dto.CreateUserRequest;
import com.example.aks.dto.UserResponse;
import com.example.aks.entity.User;
import com.example.aks.exception.ResourceNotFoundException;
import com.example.aks.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
@Tag(name = "User Management", description = "APIs for managing users")
public class UserController {

  private final UserService userService;

  @PostMapping
  @Operation(
      summary = "Create a new user",
      description =
          "Creates a new user with validation. Demonstrates payload validation according to API"
              + " spec.",
      security = @SecurityRequirement(name = "bearer-jwt"))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "User created successfully",
            content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Validation failed - invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
      })
  public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
    log.info("Creating new user: {}", request.getUsername());

    User user = userService.createUser(request);
    UserResponse response = mapToResponse(user);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "Get user by ID",
      description = "Retrieves a user by their ID. Demonstrates path variable validation.",
      security = @SecurityRequirement(name = "bearer-jwt"))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "User found",
            content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "400", description = "Invalid ID format")
      })
  public ResponseEntity<UserResponse> getUserById(@PathVariable @Min(1) Long id) {
    log.info("Fetching user with ID: {}", id);

    User user =
        userService.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

    return ResponseEntity.ok(mapToResponse(user));
  }

  @GetMapping
  @Operation(
      summary = "Get all users",
      description = "Retrieves a list of all users",
      security = @SecurityRequirement(name = "bearer-jwt"))
  @ApiResponse(
      responseCode = "200",
      description = "List of users retrieved successfully",
      content = @Content(schema = @Schema(implementation = UserResponse.class)))
  public ResponseEntity<List<UserResponse>> getAllUsers() {
    log.info("Fetching all users");

    List<UserResponse> users =
        userService.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());

    return ResponseEntity.ok(users);
  }

  @DeleteMapping("/{id}")
  @Operation(
      summary = "Delete user",
      description = "Deletes a user by their ID",
      security = @SecurityRequirement(name = "bearer-jwt"))
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "User deleted successfully"),
        @ApiResponse(responseCode = "404", description = "User not found")
      })
  public ResponseEntity<Void> deleteUser(@PathVariable @Min(1) Long id) {
    log.info("Deleting user with ID: {}", id);

    if (!userService.existsById(id)) {
      throw new ResourceNotFoundException("User", "id", id);
    }

    userService.deleteById(id);
    return ResponseEntity.noContent().build();
  }

  private UserResponse mapToResponse(User user) {
    return new UserResponse(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getUsername(), // Using username as fullName for demo
        user.getCreatedAt(),
        user.getLastLogin());
  }
}
