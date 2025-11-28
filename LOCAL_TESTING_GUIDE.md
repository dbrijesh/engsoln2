# Local Testing Guide

## Quick Start: Running the Application Locally

This guide helps you build and run the AKS Starter Kit application on your local machine for testing before deploying to Azure.

---

## Prerequisites

### Required Software

✅ **Node.js 18+ and npm** - For frontend
```bash
node --version  # Should be 18 or higher
npm --version   # Should be 8 or higher
```

✅ **Java 17+ and Maven** - For backend
```bash
java -version   # Should be 17 or higher
mvn -version    # Should be 3.6 or higher
```

✅ **Git** - For version control
```bash
git --version
```

### Optional (for full local Kubernetes testing)
- Docker Desktop
- kubectl
- Helm 3

---

## Part 1: Frontend Setup

### Step 1: Install Dependencies

```bash
cd app/frontend

# Install dependencies
npm install
```

**Note:** If you encounter TypeScript version conflicts, the package.json has been updated to use TypeScript 4.9.5 which is compatible with react-scripts 5.0.1.

### Step 2: Create Environment Configuration

Create `.env.local` file in `app/frontend/`:

```env
# Azure AD Configuration (for local testing, use mock values or set up Azure AD app)
REACT_APP_CLIENT_ID=your-frontend-client-id-here
REACT_APP_TENANT_ID=your-tenant-id-here
REACT_APP_API_URL=http://localhost:8080
REACT_APP_API_SCOPE=api://your-backend-client-id/access_as_user

# For testing without Azure AD (will show errors but app will load)
SKIP_PREFLIGHT_CHECK=true
```

### Step 3: Run Frontend Development Server

```bash
cd app/frontend

# Start development server
npm start
```

The frontend will open at: **http://localhost:3000**

**Expected Behavior:**
- React app loads successfully
- You'll see login page (may show errors if Azure AD is not configured)
- App attempts to connect to backend at http://localhost:8080

### Step 4: Build Production Bundle (Optional)

```bash
cd app/frontend

# Create production build
npm run build

# The build output will be in app/frontend/build/
```

---

## Part 2: Backend Setup

### Step 1: Configure Application Properties

Edit `app/backend/src/main/resources/application.yml` or create `application-local.yml`:

```yaml
spring:
  application:
    name: aks-starter-backend

  # For local testing, disable security or use test configuration
  security:
    oauth2:
      resourceserver:
        jwt:
          # Comment out these lines for local testing without Azure AD
          # issuer-uri: https://login.microsoftonline.com/${AZURE_TENANT_ID}/v2.0
          # audiences: ${AZURE_CLIENT_ID}

  # H2 in-memory database for local testing
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password:

  h2:
    console:
      enabled: true
      path: /h2-console

  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate:
        format_sql: true

server:
  port: 8080

# Actuator endpoints
management:
  endpoints:
    web:
      exposure:
        include: "*"
  endpoint:
    health:
      show-details: always

# Disable security for local testing (REMOVE IN PRODUCTION)
security:
  enabled: false
```

### Step 2: Option A - Build and Run with Maven

```bash
cd app/backend

# Clean and build (skip tests for faster build)
mvn clean install -DskipTests

# Run the application
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Or run the JAR directly
java -jar target/aks-starter-backend-1.0.0.jar --spring.profiles.active=local
```

### Step 2: Option B - Run from IDE

**IntelliJ IDEA:**
1. Open `app/backend` folder as project
2. Right-click on `BackendApplication.java`
3. Select "Run 'BackendApplication'"
4. Add VM option: `-Dspring.profiles.active=local`

**VS Code:**
1. Install "Spring Boot Extension Pack"
2. Open `app/backend` folder
3. Press F5 or use Run menu
4. Configure launch.json with: `"vmArgs": "-Dspring.profiles.active=local"`

**Eclipse:**
1. Import as Maven project
2. Right-click project → Run As → Spring Boot App
3. Add Program Arguments: `--spring.profiles.active=local`

### Step 3: Verify Backend is Running

Backend will start at: **http://localhost:8080**

Test the endpoints:

```bash
# Health check
curl http://localhost:8080/actuator/health

# Public health endpoint (should work without auth)
curl http://localhost:8080/actuator/health/liveness

# API endpoint (may require auth)
curl http://localhost:8080/api/hello
```

**Expected Response (Health Check):**
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "H2",
        "validationQuery": "isValid()"
      }
    }
  }
}
```

---

## Part 3: Testing the Full Application

### Step 1: Start Both Services

**Terminal 1 - Backend:**
```bash
cd app/backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

**Terminal 2 - Frontend:**
```bash
cd app/frontend
npm start
```

### Step 2: Access the Application

1. Open browser to: **http://localhost:3000**
2. Frontend should load (may show Azure AD login errors if not configured)
3. Backend API should be accessible at: **http://localhost:8080**

### Step 3: Testing Without Azure AD (Quick Test)

To test the application without setting up Azure AD:

**Modify `app/backend/src/main/java/com/example/aks/config/SecurityConfig.java`:**

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())  // Disable CSRF for local testing
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**", "/api/**", "/h2-console/**").permitAll()  // Allow all for testing
                .anyRequest().authenticated()
            )
            .headers(headers -> headers.frameOptions().disable());  // Allow H2 console

        return http.build();
    }
}
```

**Restart backend** and test:

```bash
# Should now work without authentication
curl http://localhost:8080/api/hello

# Access H2 database console
# Open: http://localhost:8080/h2-console
# JDBC URL: jdbc:h2:mem:testdb
# User: sa
# Password: (leave empty)
```

---

## Part 4: Running Tests Locally

### Frontend Tests

```bash
cd app/frontend

# Run unit tests
npm test

# Run with coverage
npm test -- --coverage --watchAll=false

# Run E2E tests (Cypress)
npm run cypress:open  # Interactive mode
npm run cypress:run   # Headless mode
```

### Backend Tests

```bash
cd app/backend

# Run all tests
mvn test

# Run unit tests only
mvn test

# Run integration tests only (includes Cucumber BDD)
mvn verify -DskipUTs=true

# Run specific test class
mvn test -Dtest=HelloServiceTest

# Run with code coverage
mvn clean verify
# Coverage report: target/site/jacoco/index.html

# View Cucumber reports after running integration tests
# Open: target/cucumber-reports/cucumber-html-reports/overview-features.html
```

### Code Quality Checks

```bash
cd app/backend

# Run Checkstyle
mvn checkstyle:check

# Check code formatting
mvn spotless:check

# Auto-format code
mvn spotless:apply

# Run all quality checks
mvn clean verify
```

---

## Part 5: Docker Local Testing

### Build Docker Images Locally

**Frontend:**
```bash
cd app/frontend

# Build Docker image
docker build -t aks-starter-frontend:local .

# Run container
docker run -p 3000:80 \
  -e REACT_APP_CLIENT_ID=your-client-id \
  -e REACT_APP_TENANT_ID=your-tenant-id \
  -e REACT_APP_API_URL=http://localhost:8080 \
  aks-starter-frontend:local

# Access at: http://localhost:3000
```

**Backend:**
```bash
cd app/backend

# Build JAR first
mvn clean package -DskipTests

# Build Docker image
docker build -t aks-starter-backend:local .

# Run container
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=local \
  aks-starter-backend:local

# Access at: http://localhost:8080
```

### Using Docker Compose

Create `docker-compose.yml` in root directory:

```yaml
version: '3.8'

services:
  backend:
    build: ./app/backend
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=local
      - AZURE_CLIENT_ID=${BACKEND_CLIENT_ID:-mock-client-id}
      - AZURE_TENANT_ID=${TENANT_ID:-mock-tenant-id}
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  frontend:
    build: ./app/frontend
    ports:
      - "3000:80"
    environment:
      - REACT_APP_CLIENT_ID=${FRONTEND_CLIENT_ID:-mock-client-id}
      - REACT_APP_TENANT_ID=${TENANT_ID:-mock-tenant-id}
      - REACT_APP_API_URL=http://localhost:8080
    depends_on:
      - backend
```

Run with Docker Compose:

```bash
# Build and start all services
docker-compose up --build

# Stop all services
docker-compose down

# View logs
docker-compose logs -f backend
docker-compose logs -f frontend
```

---

## Part 6: Troubleshooting

### Issue 1: Frontend npm install fails with TypeScript error

**Error:**
```
Could not resolve dependency: typescript@^5.3.3
```

**Solution:**
The package.json has been updated to use TypeScript 4.9.5. If issues persist:

```bash
cd app/frontend
rm -rf node_modules package-lock.json
npm install --legacy-peer-deps
```

### Issue 2: Backend Maven build fails

**Error:**
```
Failed to execute goal... Could not resolve dependencies
```

**Solution:**
```bash
cd app/backend

# Clear Maven cache
rm -rf ~/.m2/repository

# Rebuild
mvn clean install -U -DskipTests
```

### Issue 3: Port already in use

**Error:**
```
Port 8080 is already in use
```

**Solution:**

**Windows:**
```cmd
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

**Mac/Linux:**
```bash
lsof -i :8080
kill -9 <PID>
```

Or change the port:
```bash
# Backend
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081

# Frontend - edit package.json
# Change start script to: PORT=3001 react-scripts start
```

### Issue 4: CORS errors in browser

**Error:**
```
Access to fetch at 'http://localhost:8080/api/hello' from origin 'http://localhost:3000' has been blocked by CORS policy
```

**Solution:**

Add to `app/backend/src/main/java/com/example/aks/config/WebConfig.java`:

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
```

### Issue 5: Azure AD authentication errors in local testing

**Error:**
```
AADSTS7000215: Invalid client secret provided
```

**Solution:**

For local testing without Azure AD:
1. Use the modified SecurityConfig shown in Part 3, Step 3
2. Or set up Azure AD apps following GITHUB_ACTIONS_SETUP.md
3. Or use mock authentication for development

### Issue 6: H2 database not accessible

**Error:**
```
Database not found at jdbc:h2:mem:testdb
```

**Solution:**

Verify application.yml has:
```yaml
spring:
  h2:
    console:
      enabled: true
  datasource:
    url: jdbc:h2:mem:testdb
```

Access H2 console at: http://localhost:8080/h2-console

### Issue 7: Tests fail with authentication errors

**Solution:**

Disable security for tests by creating `application-test.yml`:

```yaml
spring:
  security:
    enabled: false
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
```

---

## Part 7: Useful Commands

### Quick Development Cycle

```bash
# Terminal 1: Backend with auto-reload
cd app/backend
mvn spring-boot:run -Dspring-boot.run.arguments=--spring.devtools.restart.enabled=true

# Terminal 2: Frontend with hot-reload (automatic)
cd app/frontend
npm start

# Terminal 3: Run tests on file changes
cd app/backend
mvn test -Dsurefire.rerunFailingTestsCount=2
```

### Performance Monitoring

```bash
# Backend metrics
curl http://localhost:8080/actuator/metrics

# Specific metric
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# Thread dump
curl http://localhost:8080/actuator/threaddump

# HTTP trace
curl http://localhost:8080/actuator/httptrace
```

### Database Management

```bash
# Access H2 console
open http://localhost:8080/h2-console

# Export database (via H2 console):
SCRIPT TO 'backup.sql'

# Import database (via H2 console):
RUNSCRIPT FROM 'backup.sql'
```

---

## Part 8: Local Development Best Practices

### 1. Use Profiles

Create environment-specific configuration:
- `application-local.yml` - For local development
- `application-test.yml` - For testing
- `application.yml` - Base configuration

### 2. Mock External Services

For services not available locally (Azure KeyVault, etc.):

```java
@Profile("local")
@Configuration
public class LocalConfig {

    @Bean
    public KeyVaultClient mockKeyVaultClient() {
        return new MockKeyVaultClient();
    }
}
```

### 3. Use Hot Reload

**Frontend:** Already enabled with `npm start`

**Backend:** Add Spring DevTools:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

### 4. Debug Mode

**Frontend:**
```json
// package.json
"scripts": {
  "start:debug": "BROWSER=none npm start"
}
```

**Backend:**
```bash
# IntelliJ/Eclipse: Run in Debug mode
# Maven CLI:
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"

# Then attach debugger to port 5005
```

---

## Summary

### Minimal Setup (Quick Test)

```bash
# 1. Backend
cd app/backend
mvn spring-boot:run

# 2. Frontend (new terminal)
cd app/frontend
npm install
npm start

# 3. Access
# Frontend: http://localhost:3000
# Backend: http://localhost:8080
# H2 Console: http://localhost:8080/h2-console
```

### Full Setup (Production-like)

```bash
# 1. Configure Azure AD apps
# 2. Update environment variables
# 3. Run tests
cd app/backend && mvn verify
cd app/frontend && npm test

# 4. Build Docker images
docker build -t frontend:local ./app/frontend
docker build -t backend:local ./app/backend

# 5. Run with Docker Compose
docker-compose up
```

---

## Next Steps

After successful local testing:

1. **Configure Azure AD** - Follow GITHUB_ACTIONS_SETUP.md
2. **Set up GitHub Secrets** - For CI/CD
3. **Deploy to Azure** - Run GitHub Actions workflows
4. **Monitor in Azure** - Application Insights, Log Analytics

For deployment instructions, see: **GITHUB_ACTIONS_SETUP.md**

---

## Getting Help

- **Build Issues**: Check Maven/npm logs
- **Runtime Issues**: Check application logs
- **Azure AD Issues**: See Azure AD app registration documentation
- **Deployment Issues**: See DEPLOYMENT_GUIDE.md

**Logs Location:**
- Frontend: Browser console (F12)
- Backend: Console output or `logs/spring.log`
- Docker: `docker logs <container-id>`
