// Custom exception for business logic violations
package com.example.aks.exception;

public class BusinessException extends RuntimeException {
  public BusinessException(String message) {
    super(message);
  }

  public BusinessException(String message, Throwable cause) {
    super(message, cause);
  }
}
