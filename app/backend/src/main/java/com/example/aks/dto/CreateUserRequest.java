// DTO for creating a new user with validation constraints
package com.example.aks.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a new user")
public class CreateUserRequest {

  @NotBlank(message = "Username is required")
  @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
  @Pattern(
      regexp = "^[a-zA-Z0-9_-]+$",
      message = "Username can only contain alphanumeric characters, hyphens, and underscores")
  @Schema(
      description = "Unique username",
      example = "john_doe",
      required = true,
      minLength = 3,
      maxLength = 50)
  private String username;

  @NotBlank(message = "Email is required")
  @Email(message = "Email must be valid")
  @Size(max = 100, message = "Email must not exceed 100 characters")
  @Schema(description = "User email address", example = "john.doe@example.com", required = true)
  private String email;

  @Size(max = 100, message = "Full name must not exceed 100 characters")
  @Schema(description = "User's full name", example = "John Doe")
  private String fullName;
}
