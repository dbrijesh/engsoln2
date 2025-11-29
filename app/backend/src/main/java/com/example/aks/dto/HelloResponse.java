// DTO for Hello API response
package com.example.aks.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HelloResponse {
  private String message;
  private String user;
  private LocalDateTime timestamp;
  private String version;

  public static HelloResponse create(String user, String version) {
    return new HelloResponse(
        String.format("Hello, %s! Welcome to AKS Starter Kit.", user),
        user,
        LocalDateTime.now(),
        version);
  }
}
