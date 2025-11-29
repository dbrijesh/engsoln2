// DTO for error responses
package com.example.aks.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
  private String error;
  private String message;
  private int status;
  private LocalDateTime timestamp;
  private String path;

  public static ErrorResponse create(String error, String message, int status, String path) {
    return new ErrorResponse(error, message, status, LocalDateTime.now(), path);
  }
}
