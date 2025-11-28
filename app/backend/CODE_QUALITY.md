# Code Quality and Validation Guide

This document explains the code quality tools, formatting, and validation features implemented in the backend application.

## Table of Contents
- [Checkstyle](#checkstyle)
- [Auto-Formatting with Spotless](#auto-formatting-with-spotless)
- [Payload Validation](#payload-validation)
- [Global Exception Handling](#global-exception-handling)
- [Usage Examples](#usage-examples)

## Checkstyle

Checkstyle is configured to enforce code quality standards based on Google Java Style Guide with some customizations.

### Configuration
- **Config File**: `checkstyle.xml`
- **Suppressions**: `checkstyle-suppressions.xml`

### Key Rules
- Maximum line length: 120 characters
- Maximum method length: 150 lines
- Maximum parameters: 7
- Naming conventions enforced
- Import organization required
- No star imports
- Proper whitespace and formatting

### Running Checkstyle

```bash
# Run Checkstyle checks
mvn checkstyle:check

# Generate Checkstyle report
mvn checkstyle:checkstyle

# View report at: target/site/checkstyle.html
```

### Suppressing Checkstyle Warnings

For generated code or specific cases where violations are acceptable:

```java
// CHECKSTYLE.OFF: ParameterNumber
public void methodWithManyParams(int a, int b, int c, int d, int e, int f, int g, int h) {
    // code
}
// CHECKSTYLE.ON: ParameterNumber
```

## Auto-Formatting with Spotless

Spotless automatically formats code according to Google Java Format style.

### Running Spotless

```bash
# Check formatting (fails if code is not formatted)
mvn spotless:check

# Apply formatting automatically
mvn spotless:apply

# Both checks run during compile phase
mvn compile
```

### Formatting Rules
- Google Java Format style
- Unused imports removed
- Trailing whitespace trimmed
- Files end with newline
- Import order: java, javax, org, com

### IDE Integration

**IntelliJ IDEA:**
1. Install "google-java-format" plugin
2. Settings → Editor → Code Style → Java → Scheme → Google Style

**Eclipse:**
1. Download google-java-format-eclipse plugin
2. Window → Preferences → Java → Code Style → Formatter

**VS Code:**
1. Install "Language Support for Java" extension
2. Configure formatter in settings.json:
```json
{
  "java.format.settings.url": "https://raw.githubusercontent.com/google/styleguide/gh-pages/eclipse-java-google-style.xml"
}
```

## Payload Validation

Bean Validation (JSR-380) is used to validate request payloads according to API specifications.

### Validation Annotations

Common annotations used in DTOs:

```java
@NotNull      // Field cannot be null
@NotBlank     // String cannot be null, empty, or whitespace
@NotEmpty     // Collection/array cannot be null or empty
@Size         // String/collection size constraints
@Min / @Max   // Numeric range constraints
@Email        // Valid email format
@Pattern      // Regex pattern matching
@Past / @Future  // Date/time constraints
```

### Example DTO with Validation

```java
@Data
public class CreateUserRequest {

  @NotBlank(message = "Username is required")
  @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
  @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Username can only contain alphanumeric characters")
  private String username;

  @NotBlank(message = "Email is required")
  @Email(message = "Email must be valid")
  private String email;
}
```

### Controller Validation

Use `@Valid` on request body and `@Validated` on class:

```java
@RestController
@Validated  // Enable method-level validation
public class UserController {

  @PostMapping("/users")
  public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
    // If validation fails, GlobalExceptionHandler handles it
    return ResponseEntity.ok(userService.createUser(request));
  }

  @GetMapping("/users/{id}")
  public ResponseEntity<UserResponse> getUser(@PathVariable @Min(1) Long id) {
    // Path variable validation
    return ResponseEntity.ok(userService.findById(id));
  }
}
```

### Custom Validators

Create custom validation annotations when needed:

```java
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PhoneNumberValidator.class)
public @interface ValidPhoneNumber {
  String message() default "Invalid phone number format";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};
}
```

## Global Exception Handling

The `GlobalExceptionHandler` provides centralized error handling for the entire application.

### Handled Exception Types

| Exception | Status Code | Description |
|-----------|-------------|-------------|
| `MethodArgumentNotValidException` | 400 | Request body validation failed |
| `ConstraintViolationException` | 400 | Path/query parameter validation failed |
| `HttpMessageNotReadableException` | 400 | Malformed JSON or invalid request body |
| `MethodArgumentTypeMismatchException` | 400 | Type mismatch (e.g., string for integer) |
| `ResourceNotFoundException` | 404 | Resource not found |
| `BusinessException` | 422 | Business logic violation |
| `BadCredentialsException` | 401 | Authentication failed |
| `AccessDeniedException` | 403 | Authorization failed |
| `NoHandlerFoundException` | 404 | Endpoint not found |
| `Exception` | 500 | Unexpected error |

### Error Response Format

**Standard Error Response:**
```json
{
  "error": "Resource Not Found",
  "message": "User not found with id: '123'",
  "status": 404,
  "timestamp": "2024-01-15T10:30:00",
  "path": "/api/users/123"
}
```

**Validation Error Response:**
```json
{
  "error": "Validation Failed",
  "message": "Request validation failed. Please check the errors.",
  "status": 400,
  "timestamp": "2024-01-15T10:30:00",
  "path": "/api/users",
  "fieldErrors": {
    "username": "Username must be between 3 and 50 characters",
    "email": "Email must be valid"
  }
}
```

### Custom Exceptions

Use custom exceptions for business logic:

```java
// Not found scenarios
throw new ResourceNotFoundException("User", "id", userId);

// Business rule violations
throw new BusinessException("Cannot delete user with active subscriptions");
```

## Usage Examples

### Example 1: Create User with Validation

**Request:**
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "username": "ab",
    "email": "invalid-email"
  }'
```

**Response (400 Bad Request):**
```json
{
  "error": "Validation Failed",
  "message": "Request validation failed. Please check the errors.",
  "status": 400,
  "timestamp": "2024-01-15T10:30:00",
  "path": "/api/users",
  "fieldErrors": {
    "username": "Username must be between 3 and 50 characters",
    "email": "Email must be valid"
  }
}
```

### Example 2: Valid Request

**Request:**
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "username": "john_doe",
    "email": "john.doe@example.com",
    "fullName": "John Doe"
  }'
```

**Response (201 Created):**
```json
{
  "id": 1,
  "username": "john_doe",
  "email": "john.doe@example.com",
  "fullName": "John Doe",
  "createdAt": "2024-01-15T10:30:00",
  "lastLogin": null
}
```

### Example 3: Malformed JSON

**Request:**
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{ invalid json }'
```

**Response (400 Bad Request):**
```json
{
  "error": "Invalid Request",
  "message": "Request body is malformed or contains invalid data. Please check the API specification.",
  "status": 400,
  "timestamp": "2024-01-15T10:30:00",
  "path": "/api/users"
}
```

### Example 4: Resource Not Found

**Request:**
```bash
curl -X GET http://localhost:8080/api/users/999 \
  -H "Authorization: Bearer <token>"
```

**Response (404 Not Found):**
```json
{
  "error": "Resource Not Found",
  "message": "User not found with id: '999'",
  "status": 404,
  "timestamp": "2024-01-15T10:30:00",
  "path": "/api/users/999"
}
```

## Integration with CI/CD

### Maven Build Lifecycle

Code quality checks are integrated into the build:

```bash
# Full build with all checks
mvn clean verify

# This runs:
# 1. Checkstyle validation (validate phase)
# 2. Spotless formatting check (compile phase)
# 3. Unit tests (test phase)
# 4. Code coverage (test phase)
# 5. Integration tests (verify phase)
```

### Pipeline Integration

In `azure-pipelines/build-backend.yml`:

```yaml
- task: Maven@3
  displayName: 'Build with quality checks'
  inputs:
    goals: 'clean verify'
    # Fails if:
    # - Checkstyle violations found
    # - Code not formatted correctly
    # - Tests fail
    # - Coverage below threshold
```

### Pre-commit Hooks (Optional)

Add to `.git/hooks/pre-commit`:

```bash
#!/bin/bash
mvn spotless:apply
mvn checkstyle:check
if [ $? -ne 0 ]; then
  echo "Checkstyle validation failed. Please fix violations."
  exit 1
fi
```

## Best Practices

1. **Run Spotless Before Commit**
   ```bash
   mvn spotless:apply
   ```

2. **Check Validation Early**
   - Write unit tests for DTOs with validation
   - Test both valid and invalid scenarios

3. **Use Descriptive Error Messages**
   - Custom messages in validation annotations
   - Clear business exception messages

4. **Document API Specifications**
   - Use OpenAPI annotations
   - Include validation constraints in schema

5. **Keep Checkstyle Config Updated**
   - Review and adjust rules as project evolves
   - Use suppressions sparingly

6. **Test Exception Handling**
   - Write tests for error scenarios
   - Verify error response format

## Troubleshooting

### Issue: Checkstyle Fails on Generated Code
**Solution:** Add to `checkstyle-suppressions.xml`:
```xml
<suppress checks=".*" files="[\\/]target[\\/]generated-sources[\\/]"/>
```

### Issue: Spotless Fails on Windows Line Endings
**Solution:** Configure Git to use LF:
```bash
git config core.autocrlf input
mvn spotless:apply
```

### Issue: Validation Not Working
**Solution:** Ensure:
1. `@Validated` on controller class
2. `@Valid` on request parameter
3. `spring-boot-starter-validation` dependency present

### Issue: Custom Exception Not Caught
**Solution:** Ensure:
1. Exception extends `RuntimeException`
2. `@ExceptionHandler` method exists in `GlobalExceptionHandler`
3. Exception is thrown (not caught elsewhere)

## Summary

This setup provides:
✅ **Automated code quality checks** with Checkstyle
✅ **Consistent formatting** with Spotless
✅ **Request validation** against API spec
✅ **Centralized error handling** with proper HTTP status codes
✅ **Developer-friendly** validation error messages
✅ **CI/CD integration** for automated enforcement

All quality checks run automatically during build, ensuring code standards are maintained across the team.
