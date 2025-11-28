// Global exception handler for REST API
package com.example.aks.exception;

import com.example.aks.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

  /**
   * Handle validation errors from @Valid annotation on request body.
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<ValidationErrorResponse> handleValidationException(
      MethodArgumentNotValidException ex, HttpServletRequest request) {

    log.warn("Validation error on {}: {}", request.getRequestURI(), ex.getMessage());

    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult()
        .getAllErrors()
        .forEach(
            error -> {
              String fieldName = ((FieldError) error).getField();
              String errorMessage = error.getDefaultMessage();
              errors.put(fieldName, errorMessage);
            });

    ValidationErrorResponse response =
        new ValidationErrorResponse(
            "Validation Failed",
            "Request validation failed. Please check the errors.",
            HttpStatus.BAD_REQUEST.value(),
            LocalDateTime.now(),
            request.getRequestURI(),
            errors);

    return ResponseEntity.badRequest().body(response);
  }

  /**
   * Handle constraint violations from @Validated on method parameters.
   */
  @ExceptionHandler(ConstraintViolationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<ValidationErrorResponse> handleConstraintViolation(
      ConstraintViolationException ex, HttpServletRequest request) {

    log.warn("Constraint violation on {}: {}", request.getRequestURI(), ex.getMessage());

    Map<String, String> errors =
        ex.getConstraintViolations().stream()
            .collect(
                Collectors.toMap(
                    violation -> violation.getPropertyPath().toString(),
                    ConstraintViolation::getMessage));

    ValidationErrorResponse response =
        new ValidationErrorResponse(
            "Constraint Violation",
            "Request contains invalid parameters.",
            HttpStatus.BAD_REQUEST.value(),
            LocalDateTime.now(),
            request.getRequestURI(),
            errors);

    return ResponseEntity.badRequest().body(response);
  }

  /**
   * Handle malformed JSON or invalid request body.
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex, HttpServletRequest request) {

    log.warn("Invalid request body on {}: {}", request.getRequestURI(), ex.getMessage());

    ErrorResponse response =
        ErrorResponse.create(
            "Invalid Request",
            "Request body is malformed or contains invalid data. Please check the API specification.",
            HttpStatus.BAD_REQUEST.value(),
            request.getRequestURI());

    return ResponseEntity.badRequest().body(response);
  }

  /**
   * Handle type mismatch errors (e.g., string instead of integer).
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(
      MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

    log.warn("Type mismatch on {}: {}", request.getRequestURI(), ex.getMessage());

    String message =
        String.format(
            "Parameter '%s' should be of type %s",
            ex.getName(), ex.getRequiredType().getSimpleName());

    ErrorResponse response =
        ErrorResponse.create("Type Mismatch", message, HttpStatus.BAD_REQUEST.value(), request.getRequestURI());

    return ResponseEntity.badRequest().body(response);
  }

  /**
   * Handle resource not found errors.
   */
  @ExceptionHandler(ResourceNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ResponseEntity<ErrorResponse> handleResourceNotFound(
      ResourceNotFoundException ex, HttpServletRequest request) {

    log.warn("Resource not found on {}: {}", request.getRequestURI(), ex.getMessage());

    ErrorResponse response =
        ErrorResponse.create(
            "Resource Not Found", ex.getMessage(), HttpStatus.NOT_FOUND.value(), request.getRequestURI());

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
  }

  /**
   * Handle business logic exceptions.
   */
  @ExceptionHandler(BusinessException.class)
  @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
  public ResponseEntity<ErrorResponse> handleBusinessException(
      BusinessException ex, HttpServletRequest request) {

    log.warn("Business exception on {}: {}", request.getRequestURI(), ex.getMessage());

    ErrorResponse response =
        ErrorResponse.create(
            "Business Rule Violation",
            ex.getMessage(),
            HttpStatus.UNPROCESSABLE_ENTITY.value(),
            request.getRequestURI());

    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
  }

  /**
   * Handle authentication errors.
   */
  @ExceptionHandler(BadCredentialsException.class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public ResponseEntity<ErrorResponse> handleBadCredentials(
      BadCredentialsException ex, HttpServletRequest request) {

    log.warn("Authentication failed on {}: {}", request.getRequestURI(), ex.getMessage());

    ErrorResponse response =
        ErrorResponse.create(
            "Authentication Failed",
            "Invalid credentials provided.",
            HttpStatus.UNAUTHORIZED.value(),
            request.getRequestURI());

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
  }

  /**
   * Handle authorization errors.
   */
  @ExceptionHandler(AccessDeniedException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ResponseEntity<ErrorResponse> handleAccessDenied(
      AccessDeniedException ex, HttpServletRequest request) {

    log.warn("Access denied on {}: {}", request.getRequestURI(), ex.getMessage());

    ErrorResponse response =
        ErrorResponse.create(
            "Access Denied",
            "You do not have permission to access this resource.",
            HttpStatus.FORBIDDEN.value(),
            request.getRequestURI());

    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
  }

  /**
   * Handle 404 - endpoint not found.
   */
  @ExceptionHandler(NoHandlerFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ResponseEntity<ErrorResponse> handleNoHandlerFound(
      NoHandlerFoundException ex, HttpServletRequest request) {

    log.warn("Endpoint not found: {}", request.getRequestURI());

    ErrorResponse response =
        ErrorResponse.create(
            "Endpoint Not Found",
            String.format("The endpoint '%s' does not exist.", request.getRequestURI()),
            HttpStatus.NOT_FOUND.value(),
            request.getRequestURI());

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
  }

  /**
   * Handle all other uncaught exceptions.
   */
  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ResponseEntity<ErrorResponse> handleGenericException(
      Exception ex, HttpServletRequest request) {

    log.error("Unexpected error on {}: {}", request.getRequestURI(), ex.getMessage(), ex);

    ErrorResponse response =
        ErrorResponse.create(
            "Internal Server Error",
            "An unexpected error occurred. Please try again later.",
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            request.getRequestURI());

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }
}
