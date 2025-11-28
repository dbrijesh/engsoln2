# Customization Checklist

This checklist guides you through adapting the AKS Starter Kit for your own application. Follow these steps to replace the "Hello World" sample with your actual application code.

## Phase 1: Planning & Preparation

### 1.1 Define Your Application

- [ ] Document application requirements
- [ ] Identify frontend technology stack (or keep React)
- [ ] Identify backend technology stack (or keep Spring Boot)
- [ ] List all external dependencies (databases, APIs, etc.)
- [ ] Define environment-specific configurations (dev, stage, prod)
- [ ] Identify required Azure services beyond what's included
- [ ] Document authentication/authorization requirements
- [ ] List all required secrets and configuration values

### 1.2 Azure Entra (Azure AD) Setup

- [ ] Create backend API app registration in Azure AD
- [ ] Note the backend client ID and tenant ID
- [ ] Configure API scopes for backend
- [ ] Create frontend app registration in Azure AD
- [ ] Note the frontend client ID
- [ ] Configure redirect URIs for all environments
- [ ] Grant API permissions (frontend → backend)
- [ ] Test authentication flow manually

### 1.3 Azure Subscription Preparation

- [ ] Decide on resource naming conventions
- [ ] Choose Azure regions (primary and secondary)
- [ ] Determine resource group strategy
- [ ] Set up billing alerts
- [ ] Configure subscription-level Azure policies
- [ ] Create service principal for Terraform (if not using managed identity)

## Phase 2: Project Customization

### 2.1 Update Project Naming

**Files to modify**:
```
├── README.md - Update project name and description
├── infra/variables.tf - Change project_name default value
├── infra/envs/*/main.tf - Update project_name variable
├── charts/frontend/Chart.yaml - Update name and description
├── charts/backend/Chart.yaml - Update name and description
├── package.json (frontend) - Update name field
├── pom.xml (backend) - Update artifactId and name
```

**Tasks**:
- [ ] Replace "aksstarter" with your project name (lowercase, no spaces)
- [ ] Replace "AKS Starter" with your project display name
- [ ] Update descriptions and metadata
- [ ] Update maintainer information
- [ ] Update repository URLs (if applicable)

### 2.2 Replace Frontend Application

**Option A: Keep React, Replace Code**

Files to modify:
- [ ] `app/frontend/src/App.js` - Replace with your app logic
- [ ] `app/frontend/src/components/*` - Replace/add your components
- [ ] `app/frontend/src/authConfig.js` - Update with your Azure AD IDs
- [ ] `app/frontend/package.json` - Add your dependencies
- [ ] `app/frontend/public/index.html` - Update title and meta tags

**Option B: Different Frontend Stack (Angular, Vue, etc.)**

- [ ] Replace entire `app/frontend/` directory
- [ ] Keep Dockerfile structure (multi-stage build)
- [ ] Ensure nginx configuration includes:
  - [ ] Health endpoint at `/health`
  - [ ] Security headers
  - [ ] SPA routing support
- [ ] Update `app/frontend/package.json` test scripts
- [ ] Update `azure-pipelines/build-frontend.yml` with correct build commands
- [ ] Update Cypress tests or replace with your E2E framework

### 2.3 Replace Backend Application

**Option A: Keep Spring Boot, Replace Logic**

Files to modify:
- [ ] `app/backend/src/main/java/com/example/aks/` - Replace with your package
- [ ] Update package structure to match your domain
- [ ] Replace controllers with your API endpoints
- [ ] Replace services with your business logic
- [ ] Add your entities and repositories
- [ ] Update `application.yml` with your config
- [ ] Add your database configuration (if needed)
- [ ] Update OpenAPI documentation
- [ ] Replace test files with your tests
- [ ] Update Cucumber feature files

**Option B: Different Backend Stack (Node.js, .NET, Go, etc.)**

- [ ] Replace entire `app/backend/` directory
- [ ] Ensure Dockerfile follows best practices:
  - [ ] Multi-stage build
  - [ ] Non-root user
  - [ ] Health check
  - [ ] Optimized layers
- [ ] Implement health endpoints:
  - [ ] `/actuator/health` or equivalent
  - [ ] Liveness probe endpoint
  - [ ] Readiness probe endpoint
- [ ] Update `azure-pipelines/build-backend.yml`:
  - [ ] Change build tool (Maven → npm/gradle/dotnet)
  - [ ] Update test commands
  - [ ] Update code coverage tool
  - [ ] Update SonarQube configuration

### 2.4 Update Configuration Files

**Frontend Environment Variables**:

File: `app/frontend/.env.example`
- [ ] `REACT_APP_CLIENT_ID` - Your frontend Azure AD app ID
- [ ] `REACT_APP_TENANT_ID` - Your Azure AD tenant ID
- [ ] `REACT_APP_API_URL` - Your backend API URL
- [ ] `REACT_APP_API_SCOPE` - Your backend API scope
- [ ] Add any additional environment variables

**Backend Configuration**:

File: `app/backend/src/main/resources/application.yml`
- [ ] Update `spring.security.oauth2` configuration
- [ ] Add database configuration (if using a database)
- [ ] Update CORS origins
- [ ] Add external API configurations
- [ ] Configure logging levels
- [ ] Add application-specific properties

**Helm Charts**:

Files: `charts/frontend/values.yaml` and `charts/backend/values.yaml`
- [ ] Update `image.repository` to your ACR name
- [ ] Update `ingress.hosts` to your domain names
- [ ] Update resource limits based on your app's needs
- [ ] Update HPA thresholds
- [ ] Update environment variables
- [ ] Add any additional secrets or config maps
- [ ] Update health probe paths if different

## Phase 3: Infrastructure Customization

### 3.1 Terraform Variables

File: `infra/terraform.tfvars.example`

Required updates:
- [ ] `project_name` - Your project identifier
- [ ] `location` - Your preferred Azure region
- [ ] `tags` - Your organization's tagging requirements
- [ ] `apim_publisher_email` - Your team's email
- [ ] Adjust VM sizes based on your workload
- [ ] Adjust node counts for each environment
- [ ] Update Kubernetes version if needed

### 3.2 Add Additional Azure Resources

If your application needs additional services:

**Database** (Example: Azure Database for PostgreSQL):
- [ ] Create new Terraform module in `infra/modules/postgresql/`
- [ ] Add module reference in `infra/main.tf`
- [ ] Add connection string to Key Vault
- [ ] Update backend to use database
- [ ] Add database migration scripts
- [ ] Update Helm charts with DB connection config

**Cache** (Example: Azure Redis):
- [ ] Create Terraform module for Redis
- [ ] Add to main.tf
- [ ] Configure connection in backend
- [ ] Update application to use cache

**Storage** (Example: Azure Storage Account):
- [ ] Create Terraform module
- [ ] Add access configuration
- [ ] Update application with SDK
- [ ] Add to Helm values

**Message Queue** (Example: Azure Service Bus):
- [ ] Create Terraform module
- [ ] Configure topics/queues
- [ ] Update application code
- [ ] Add consumer configurations

### 3.3 Network Configuration

If you need custom networking:
- [ ] Update `aks_network_profile` in variables
- [ ] Create VNet peering if needed
- [ ] Configure NSG rules
- [ ] Set up private endpoints for PaaS services
- [ ] Configure ExpressRoute or VPN (if required)

### 3.4 Security & Compliance

- [ ] Review and update WAF rules in `infra/modules/appgw_waf/`
- [ ] Configure Azure Policy definitions
- [ ] Set up diagnostic settings for audit logs
- [ ] Configure Azure Security Center
- [ ] Set up Azure Sentinel (if required)
- [ ] Review RBAC assignments

## Phase 4: CI/CD Customization

### 4.1 Azure DevOps Setup

**Service Connections**:
- [ ] Create Azure Resource Manager connection (name: `azure-service-connection`)
- [ ] Create ACR Docker Registry connection (name: `acr-service-connection`)
- [ ] Create SonarQube connection if using (name: `sonarqube-service-connection`)
- [ ] Create any additional service connections for external services

**Environments**:
- [ ] Create `dev` environment
- [ ] Create `stage` environment with approvals
- [ ] Create `prod` environment with approvals and checks
- [ ] Configure environment-specific variables

### 4.2 Update Pipeline Variables

File: `azure-pipelines/build-frontend.yml`
- [ ] Update `acrName` variable
- [ ] Update `acrServiceConnection` if you used a different name
- [ ] Add any additional build steps
- [ ] Update test commands if changed
- [ ] Update coverage thresholds

File: `azure-pipelines/build-backend.yml`
- [ ] Update `acrName` variable
- [ ] Change build tool if not using Maven
- [ ] Update test execution
- [ ] Add database setup for integration tests (if needed)
- [ ] Update coverage requirements

File: `azure-pipelines/infra-deploy.yml`
- [ ] Update `tfstateResourceGroup`
- [ ] Update `tfstateStorageAccount`
- [ ] Add post-deployment steps if needed

File: `azure-pipelines/release-deploy.yml`
- [ ] Update resource group and cluster naming
- [ ] Add database migration steps (if applicable)
- [ ] Add smoke tests specific to your app
- [ ] Configure blue-green or canary deployment (if needed)
- [ ] Add post-deployment validation

### 4.3 Testing Updates

**Unit Tests**:
- [ ] Update test coverage thresholds in `package.json` and `pom.xml`
- [ ] Add tests for your features
- [ ] Configure test data setup/teardown

**Integration Tests**:
- [ ] Update Cucumber feature files in `app/backend/src/test/resources/features/`
- [ ] Update Cypress tests in `app/frontend/cypress/e2e/`
- [ ] Add test scenarios for your business logic
- [ ] Configure test environment data

**Security Scans**:
- [ ] Review Trivy scan configuration
- [ ] Customize tfsec rules if needed
- [ ] Configure OWASP ZAP baseline (create `zap-baseline-config.conf`)
- [ ] Set acceptable risk thresholds

### 4.4 SonarQube Configuration

Files: `app/frontend/sonar-project.properties` and `app/backend/sonar-project.properties`
- [ ] Update `sonar.projectKey`
- [ ] Update `sonar.projectName`
- [ ] Adjust code coverage thresholds
- [ ] Add exclusions for generated code
- [ ] Configure quality gate requirements

## Phase 5: Secrets Management

### 5.1 Identify All Secrets

Create a list of all secrets needed:
- [ ] Azure AD client IDs and secrets
- [ ] Database connection strings
- [ ] External API keys
- [ ] TLS certificates
- [ ] Service principal credentials
- [ ] Third-party service tokens
- [ ] Encryption keys

### 5.2 Store Secrets in Azure Key Vault

For each secret:
- [ ] Add to Key Vault via Terraform or Azure CLI
- [ ] Document secret name and purpose
- [ ] Configure access policies or RBAC
- [ ] Set rotation reminders (if applicable)
- [ ] Update Helm charts to reference secrets

### 5.3 Configure Kubernetes Secrets

- [ ] Create secret manifests or use Key Vault CSI driver
- [ ] Update Helm charts to mount secrets
- [ ] Test secret access from pods
- [ ] Document secret rotation procedures

## Phase 6: Monitoring & Observability

### 6.1 Application Insights

- [ ] Add Application Insights SDK to your application
- [ ] Configure instrumentation key from Key Vault
- [ ] Add custom telemetry for business events
- [ ] Configure sampling if needed
- [ ] Set up availability tests

### 6.2 Logging

- [ ] Implement structured logging (JSON)
- [ ] Add correlation IDs to requests
- [ ] Configure log levels per environment
- [ ] Set up log retention policies
- [ ] Create log queries for common scenarios

### 6.3 Metrics & Alerts

- [ ] Define key performance indicators (KPIs)
- [ ] Expose Prometheus metrics
- [ ] Create custom metrics for business logic
- [ ] Set up alert rules in Azure Monitor:
  - [ ] Service availability
  - [ ] Error rates
  - [ ] Response times
  - [ ] Resource utilization
  - [ ] Custom business metrics
- [ ] Configure notification channels (email, Teams, PagerDuty)

### 6.4 Dashboards

- [ ] Create Azure Monitor workbooks
- [ ] Create Grafana dashboards (if using)
- [ ] Build executive dashboards for business metrics
- [ ] Set up cost dashboards

## Phase 7: Documentation

### 7.1 Update Documentation

- [ ] Update README.md with project-specific information
- [ ] Update docs/QUICKSTART.md with your setup steps
- [ ] Update docs/architecture.md with your architecture
- [ ] Create API documentation (OpenAPI/Swagger)
- [ ] Document custom configurations
- [ ] Create runbooks for common operations
- [ ] Document troubleshooting procedures

### 7.2 Create Operational Documents

- [ ] Deployment procedures
- [ ] Rollback procedures
- [ ] Incident response plan
- [ ] Disaster recovery plan
- [ ] Backup and restore procedures
- [ ] Scaling procedures
- [ ] Database migration procedures (if applicable)

## Phase 8: Testing & Validation

### 8.1 Local Testing

- [ ] Build and run frontend locally
- [ ] Build and run backend locally
- [ ] Test authentication flow
- [ ] Test all API endpoints
- [ ] Run all unit tests
- [ ] Run integration tests
- [ ] Build Docker images successfully
- [ ] Test with local Kubernetes (Minikube/Kind)

### 8.2 Dev Environment Testing

- [ ] Deploy infrastructure to dev
- [ ] Deploy applications to dev AKS
- [ ] Verify all pods are running
- [ ] Test health endpoints
- [ ] Test application functionality
- [ ] Verify monitoring and logging
- [ ] Run security scans
- [ ] Perform load testing
- [ ] Test auto-scaling
- [ ] Test failover scenarios

### 8.3 Pipeline Testing

- [ ] Run frontend build pipeline
- [ ] Run backend build pipeline
- [ ] Run infrastructure pipeline
- [ ] Run release pipeline
- [ ] Verify all tests pass
- [ ] Verify security scans complete
- [ ] Verify artifacts are published
- [ ] Test manual approval gates

## Phase 9: Production Preparation

### 9.1 Security Hardening

- [ ] Review all firewall rules
- [ ] Enable Azure Security Center recommendations
- [ ] Configure private endpoints for PaaS services
- [ ] Review and lock down NSG rules
- [ ] Enable Azure DDoS Protection Standard (if needed)
- [ ] Configure WAF in Prevention mode
- [ ] Review RBAC assignments (principle of least privilege)
- [ ] Enable diagnostic logs for all resources
- [ ] Set up security alerts
- [ ] Perform penetration testing

### 9.2 Performance Optimization

- [ ] Right-size AKS node pools
- [ ] Configure HPA thresholds based on load testing
- [ ] Optimize database queries
- [ ] Enable caching where appropriate
- [ ] Configure CDN for static assets
- [ ] Optimize container images (minimize size)
- [ ] Review and optimize resource limits

### 9.3 Disaster Recovery

- [ ] Document RTO and RPO requirements
- [ ] Set up geo-replication (if required)
- [ ] Test backup and restore procedures
- [ ] Create DR runbook
- [ ] Test failover procedures
- [ ] Document recovery steps

### 9.4 Compliance

- [ ] Review compliance requirements (GDPR, HIPAA, etc.)
- [ ] Configure data retention policies
- [ ] Set up audit logging
- [ ] Document data flows
- [ ] Create privacy policy documents
- [ ] Implement data encryption requirements

## Phase 10: Go-Live

### 10.1 Pre-Launch Checklist

- [ ] All tests passing in stage environment
- [ ] Security scans show no critical issues
- [ ] Performance meets requirements
- [ ] Monitoring and alerting configured
- [ ] Documentation complete
- [ ] Team trained on operations
- [ ] Support processes in place
- [ ] Backup and DR tested

### 10.2 Launch

- [ ] Deploy to production
- [ ] Smoke test production environment
- [ ] Verify monitoring
- [ ] Monitor for issues
- [ ] Communicate launch to stakeholders

### 10.3 Post-Launch

- [ ] Monitor for 24-48 hours
- [ ] Address any issues
- [ ] Collect metrics
- [ ] Gather user feedback
- [ ] Create post-mortem document
- [ ] Plan improvements

## Quick Reference: Files to Change

### Minimum Required Changes

```
Priority 1 (Must Change):
├── app/frontend/src/authConfig.js - Azure AD IDs
├── app/backend/src/main/resources/application.yml - Azure AD config
├── infra/terraform.tfvars.example - Project name and ACR name
├── charts/*/values.yaml - Image repository and ingress hosts

Priority 2 (Should Change):
├── All package.json/pom.xml - Project names
├── All Chart.yaml - Chart names
├── README.md - Project description
├── azure-pipelines/*.yml - Variable values

Priority 3 (Replace with Your Code):
├── app/frontend/src/* - Your React components
├── app/backend/src/main/java/* - Your business logic
├── app/*/tests/* - Your test cases
```

## Getting Help

If you get stuck:
1. Review the QUICKSTART.md guide
2. Check the architecture.md for design decisions
3. Review Azure/Kubernetes documentation
4. Check Azure DevOps pipeline logs
5. Use `kubectl describe` and `kubectl logs` for debugging
6. Review Terraform plan output carefully

## Success Criteria

You've successfully customized the starter kit when:
- ✅ Your application builds and runs locally
- ✅ All pipelines run successfully
- ✅ Infrastructure deploys without errors
- ✅ Applications deploy to AKS
- ✅ Authentication works end-to-end
- ✅ All tests pass
- ✅ Monitoring shows data
- ✅ Security scans pass
- ✅ Documentation is updated
- ✅ Team can deploy and operate the solution
