// DTO for user response
package com.example.aks.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User information response")
public class UserResponse {

  @Schema(description = "User ID", example = "1")
  private Long id;

  @Schema(description = "Username", example = "john_doe")
  private String username;

  @Schema(description = "Email address", example = "john.doe@example.com")
  private String email;

  @Schema(description = "Full name", example = "John Doe")
  private String fullName;

  @Schema(description = "Account creation timestamp")
  private LocalDateTime createdAt;

  @Schema(description = "Last login timestamp")
  private LocalDateTime lastLogin;
}
