// DTO for validation error responses with field-level errors
package com.example.aks.dto;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ValidationErrorResponse extends ErrorResponse {
  private Map<String, String> fieldErrors;

  public ValidationErrorResponse(
      String error,
      String message,
      int status,
      LocalDateTime timestamp,
      String path,
      Map<String, String> fieldErrors) {
    super(error, message, status, timestamp, path);
    this.fieldErrors = fieldErrors;
  }
}
