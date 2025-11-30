# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Overview

This is a production-ready Azure AKS Enterprise Starter Kit - a fully reusable engineering solution for deploying applications to Azure Kubernetes Service with complete CI/CD automation. The repository includes a React frontend, Spring Boot backend, Terraform infrastructure, Helm charts, and Azure DevOps pipelines.

## Build, Test, and Run Commands

### Local Development (via Makefile)

```bash
# Install dependencies
make install              # Installs npm packages (frontend) and Maven dependencies (backend)

# Build applications
make build               # Builds both frontend (npm run build) and backend (mvn clean package -DskipTests)

# Run tests
make test                # Runs frontend Jest tests and backend JUnit/Cucumber tests
cd app/frontend && npm test                    # Frontend tests only (40% coverage threshold)
cd app/backend && ./mvnw test                  # Backend unit tests only
cd app/backend && ./mvnw verify                # Backend with integration tests (Cucumber BDD)

# Code quality
make quality-check       # Runs Checkstyle and Spotless validation
make format             # Auto-formats backend code with Google Java Format

# Docker local development
make docker-build       # Builds Docker images with 'local' tag
make docker-up          # Starts services via docker-compose (frontend: localhost:3000, backend: localhost:8080)
make docker-down        # Stops Docker Compose services

# Kubernetes local deployment
make helm-install       # Installs Helm charts to local K8s cluster (dev namespace)
make helm-uninstall     # Removes Helm deployments
```

### Running Individual Tests

```bash
# Frontend
cd app/frontend
npm test                           # All tests with coverage
npm run test:watch                 # Watch mode for development
npm run cypress:open               # E2E tests interactive mode
npm run cypress:run                # E2E tests headless

# Backend
cd app/backend
./mvnw test                        # Unit tests only (*Test.java)
./mvnw verify                      # Unit + integration tests (*IT.java, Cucumber)
./mvnw test -Dtest=HelloControllerTest  # Single test class
./mvnw clean verify -Dcheckstyle.skip -Dspotless.skip  # Skip quality checks
```

### Terraform Infrastructure

```bash
make tf-init ENV=dev     # Initialize Terraform for dev environment
make tf-plan ENV=dev     # Plan infrastructure changes
make tf-apply ENV=dev    # Apply infrastructure changes
make destroy ENV=dev     # Destroy all Azure resources

# Or manually:
cd infra/envs/dev
terraform init -backend-config="backend-dev.hcl"
terraform plan
terraform apply
```

## Code Architecture

### Frontend (React 18 SPA)

**Location**: `app/frontend/`

**Key Pattern: Runtime Environment Variable Injection**
- The frontend uses `window._env_` for dynamic configuration instead of build-time variables
- `env.sh` script (in Dockerfile) generates `env-config.js` at container startup
- This enables deploying the same Docker image to dev/stage/prod with different credentials
- Pattern in code: `const getEnv = (key) => (window._env_?.[key]) || process.env[key]`

**Authentication (Azure Entra SSO)**
- Uses `@azure/msal-react` and `@azure/msal-browser` for OAuth2 PKCE flow
- `authConfig.js` configures MSAL with CLIENT_ID, TENANT_ID from runtime env
- Token acquisition: silent acquisition with fallback to interactive popup
- API calls include `Authorization: Bearer {token}` header via Axios

**Component Structure**
- `App.js`: Root with `MsalProvider`, conditional rendering via `AuthenticatedTemplate`
- `Home.js`: Demonstrates API calls with token acquisition
- Simple routing with `react-router-dom` v6
- Nginx serves SPA with fallback for client-side routing

**Testing**
- Jest with 40% minimum coverage (branches/lines/statements ~40%, functions 30%)
- Coverage excludes `index.js` and `reportWebVitals.js`
- Cypress for E2E tests (v13.6.2)
- ESLint for code quality

### Backend (Spring Boot 3.2.0)

**Location**: `app/backend/`

**Security Architecture**
- `SecurityConfig.java`: Stateless JWT validation with OAuth2 Resource Server
- Public paths: `/actuator/**`, `/swagger-ui/**`, `/v3/api-docs/**`, `/api/public/**`
- `CustomJwtAuthenticationConverter.java`: Extracts Azure AD roles from JWT claims and converts to Spring `ROLE_*` authorities
- CORS configured via `app.cors.allowed-origins` environment variable

**Resilience Patterns (Resilience4j)**
- Circuit Breaker: `@CircuitBreaker` annotation with fallback methods (50% failure threshold, 10s wait)
- Retry: `@Retry` annotation for transient failures
- Rate Limiting: `@RateLimiter(name = "api")` - 100 req/sec limit
- See `HelloService.java` for fallback pattern example

**REST API & Documentation**
- OpenAPI/Swagger UI available at `/swagger-ui/`
- Controllers: `HelloController.java`, `UserController.java` with proper validation
- `GlobalExceptionHandler.java`: Centralized error handling with `@ControllerAdvice`
  - Returns structured `ErrorResponse` with timestamp, path, details
  - Handles validation errors, authentication errors, business exceptions

**Health & Observability**
- Liveness probe: `/actuator/health/liveness` (detects deadlocks)
- Readiness probe: `/actuator/health/readiness` (checks dependencies)
- Prometheus metrics: `/actuator/metrics` and `/actuator/prometheus`
- Helm charts use these for Kubernetes health checks

**Testing Strategy**
- JUnit 5 for unit tests (15% minimum coverage via JaCoCo)
- Cucumber BDD: Gherkin feature files in `src/test/resources/features/`
- Step definitions use REST Assured for API testing
- Test profile (`@ActiveProfiles("test")`) disables OAuth2 requirements
- Maven Surefire: unit tests (*Test.java)
- Maven Failsafe: integration tests (*IT.java)

**Code Quality Tools**
- **Spotless**: Auto-formats code with Google Java Format (runs at `process-sources` phase)
- **Checkstyle**: Code quality checks (warning-only, non-blocking)
- Spotless runs BEFORE Checkstyle to ensure formatting is correct

**Profiles**
- `application.yml`: Production defaults
- `application-local.yml`: Local development (security disabled)
- `application-test.yml`: Test environment
- Activate via `SPRING_PROFILES_ACTIVE` env var

### Infrastructure (Terraform)

**Location**: `infra/`

**Module Structure** (`infra/modules/`)
- `aks/`: Kubernetes cluster with AAD RBAC, CSI driver for Key Vault, cluster autoscaler
- `acr/`: Container registry with image retention
- `keyvault/`: Secrets management with soft-delete protection
- `appgw_waf/`: Application Gateway with WAF v2 for OWASP protection
- `apim/`: API Management for API governance
- `monitoring/`: Log Analytics workspace

**Environment Separation** (`infra/envs/`)
- Three environments: `dev/`, `stage/`, `prod/`
- Each has `main.tf` calling root module with environment-specific values
- `backend-{env}.hcl` for Terraform state configuration
- State files: `{environment}.terraform.tfstate`

**Key Features**
- AKS with Azure AD RBAC enabled
- Managed identity for kubelet (ACR pull access)
- Key Vault CSI driver addon for mounting secrets
- Cluster autoscaler: min 2, max 5+ nodes (configurable per env)
- Network policy with Azure CNI

### Kubernetes Deployment (Helm)

**Location**: `charts/`

**Frontend Chart** (`charts/frontend/`)
```yaml
replicaCount: 2
resources: 250m CPU / 256Mi memory (request), 500m / 512Mi (limit)
autoscaling: min 2, max 10 replicas (70% CPU / 80% memory threshold)
probes: liveness & readiness at /health (30s/10s initial delay)
podDisruptionBudget: minAvailable 1
```

**Runtime environment injection**: Helm injects env vars (REACT_APP_CLIENT_ID, REACT_APP_TENANT_ID, etc.) from Kubernetes secrets referenced in values.yaml

**Backend Chart** (`charts/backend/`)
```yaml
replicaCount: 3 (higher HA requirement)
resources: 500m CPU / 512Mi memory (request), 1000m / 1Gi (limit)
autoscaling: min 3, max 15 replicas
probes: /actuator/health/liveness (60s initial for JVM startup), /actuator/health/readiness (30s)
podDisruptionBudget: minAvailable 2
```

**Key Vault CSI Driver**: Optional `secretProviderClass` for mounting Azure Key Vault secrets

**Workload Identity**: ServiceAccount annotation for pod-to-Azure authentication

**Common Patterns**
- Helper templates in `_helpers.tpl` for consistent naming and labels
- Pod anti-affinity rules to spread replicas across nodes
- Ingress with NGINX controller and cert-manager for self-signed TLS
- IP-based ingress support (no DNS required)

### CI/CD Pipelines (Azure DevOps)

**Location**: `azure-pipelines/`

**Build Pipelines**
- `build-frontend.yml`: npm install → lint → test → SonarQube → Docker build → Trivy scan → push to ACR
- `build-backend.yml`: Checkstyle → Spotless → Maven test → JaCoCo coverage → SonarQube → Docker build → Trivy scan → push to ACR
- Triggered on relevant file path changes only

**Infrastructure Pipeline** (`infra-deploy.yml`)
- Validate: terraform fmt check → validate → tflint → tfsec
- Plan: terraform plan with artifact publication
- Apply: requires approval, applies plan to environment

**Release Pipeline** (`release-deploy.yml`)
- Deploys backend and frontend Helm charts to AKS
- Parameters: environment (dev/stage/prod), imageTag
- Manual deployment with environment-specific approval gates

**Artifact Management**
- Test results, coverage reports, security scan results published
- Terraform plans preserved for review before apply

## Important Development Patterns

### 1. Runtime Environment Variable Injection (Frontend)

**Why**: Docker images are immutable; credentials cannot be embedded at build time.

**How**: The `env.sh` script in the frontend Dockerfile generates `env-config.js` at container startup by reading environment variables. React app checks `window._env_` before falling back to `process.env`.

**Impact**: Deploy the same image to dev/stage/prod with different configurations via Helm values.

### 2. Resilience4j Fallback Pattern (Backend)

Services return degraded responses instead of failing when external dependencies are unavailable. Example in `HelloService.java`:

```java
@CircuitBreaker(name = "externalService", fallbackMethod = "getFallbackHelloMessage")
public HelloResponse getHelloMessage(String username) { ... }

public HelloResponse getFallbackHelloMessage(String username, Exception ex) {
  return new HelloResponse("Hello, " + username + "! (Fallback response)", ...);
}
```

### 3. Centralized Exception Handling (Backend)

`GlobalExceptionHandler.java` provides consistent error responses across all endpoints:
- Validation errors return field-level details
- Authentication/authorization errors return 401/403
- Business exceptions return 422
- All responses include timestamp, request path, error details

### 4. Multi-Stage Docker Builds

Both frontend and backend use multi-stage builds for size optimization:
- Build stage: Compile/package application
- Runtime stage: Lean base image (nginx-alpine or jre-alpine)
- Non-root user for security

### 5. Profile-Based Configuration (Backend)

Use Spring profiles to adapt configuration:
- `SPRING_PROFILES_ACTIVE=local`: Disables security for local development
- `SPRING_PROFILES_ACTIVE=test`: Used in tests
- `SPRING_PROFILES_ACTIVE=prod`: Production defaults

### 6. Helm Value Overrides

Deploy to different environments by overriding values:

```bash
helm upgrade --install backend ./charts/backend \
  --set image.tag=build-123 \
  --set env.CORS_ALLOWED_ORIGINS=https://frontend.example.com \
  --set replicaCount=5
```

### 7. ACR Authentication

AKS nodes pull images from ACR using managed identity (no credentials needed). This is established via Terraform role assignment in `infra/modules/aks/`.

## Common Workflows

### Adding a New API Endpoint (Backend)

1. Create controller method with OpenAPI annotations
2. Add security annotations if needed (`@PreAuthorize`)
3. Write unit test in `src/test/java/`
4. Add Cucumber scenario in `src/test/resources/features/`
5. Run `./mvnw verify` to ensure tests pass
6. Auto-format: `./mvnw spotless:apply`

### Adding a New Frontend Component

1. Create component in `src/components/`
2. Add routing in `App.js` if needed
3. Use `useMsal()` hook for authentication context
4. Write Jest tests in component file or `tests/` directory
5. Run `npm test` to verify coverage thresholds
6. Run `npm run lint` to check code quality

### Modifying Infrastructure

1. Update Terraform modules in `infra/modules/` or environment configs in `infra/envs/`
2. Run `make tf-init ENV=dev` and `make tf-plan ENV=dev`
3. Review plan output
4. Apply with `make tf-apply ENV=dev`
5. In CI/CD, this triggers `infra-deploy.yml` pipeline with approval gates

### Deploying to AKS

1. Build and push images via build pipelines (automatic on git push)
2. Update Helm values if needed in `charts/*/values.yaml`
3. Run release pipeline with environment and imageTag parameters
4. Or manually: `helm upgrade --install {release} ./charts/{app} --set image.tag={tag}`

## Key Files to Know

- `app/frontend/src/authConfig.js`: Azure AD authentication configuration
- `app/frontend/public/env.sh`: Runtime environment variable injection script
- `app/backend/src/main/java/com/example/aks/config/SecurityConfig.java`: Spring Security configuration
- `app/backend/src/main/java/com/example/aks/exception/GlobalExceptionHandler.java`: Centralized error handling
- `app/backend/src/main/resources/application.yml`: Backend configuration
- `infra/envs/{dev|stage|prod}/main.tf`: Environment-specific infrastructure
- `charts/*/values.yaml`: Helm deployment configuration
- `docker-compose.yml`: Local development with both services
- `Makefile`: Common development commands

## Testing Requirements

### Frontend
- Minimum coverage: 40% branches/lines/statements, 30% functions
- Jest configuration in `package.json`
- E2E tests with Cypress

### Backend
- Minimum coverage: 15% line coverage (JaCoCo)
- Unit tests: `*Test.java` classes
- Integration tests: `*IT.java` classes and Cucumber scenarios
- Code formatting enforced by Spotless (Google Java Format)
- Checkstyle runs as warnings only

## Security Considerations

- Never commit credentials or secrets to the repository
- Secrets stored in Azure Key Vault, referenced via Kubernetes secrets or CSI driver
- Frontend: OAuth2 PKCE flow (no client secret)
- Backend: JWT validation with Azure AD issuer
- CORS properly configured per environment
- Container images scanned with Trivy before deployment
- Infrastructure scanned with tfsec and tflint
- WAF v2 enabled for OWASP top 10 protection

## Cost Management

Estimated monthly costs for dev environment: $350-650
- To minimize: use smaller VM SKUs, scale down when not in use
- Cleanup: `make destroy ENV=dev` or `./scripts/cleanup-azure.sh dev`

## Troubleshooting Tips

### Frontend authentication issues
- Check `window._env_` in browser console
- Verify REACT_APP_CLIENT_ID and REACT_APP_TENANT_ID match Azure AD app
- Check CORS configuration if API calls fail

### Backend authentication issues
- Verify `AZURE_AD_ISSUER_URI` matches tenant ID
- Check JWT token claims in https://jwt.ms
- Ensure CustomJwtAuthenticationConverter extracts roles correctly

### Kubernetes deployment issues
- Check pod logs: `kubectl logs -n {namespace} {pod-name}`
- Verify health probes: `kubectl describe pod -n {namespace} {pod-name}`
- Check resource limits and HPA status
- Ensure secrets are properly mounted

### Terraform issues
- State file conflicts: ensure backend config is correct
- Module dependencies: check `terraform graph`
- RBAC issues: verify Azure CLI authentication and permissions
