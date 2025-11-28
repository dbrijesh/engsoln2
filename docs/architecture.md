# Architecture Documentation

## System Overview

The AKS Starter Kit is a production-grade, enterprise-ready reference architecture for deploying containerized applications on Azure Kubernetes Service (AKS) with complete CI/CD automation.

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Internet Users                               │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             ▼
                   ┌─────────────────────┐
                   │  Azure Front Door   │
                   │  + WAF v2           │
                   │  (DDoS Protection)  │
                   └──────────┬──────────┘
                              │
                              ▼
                   ┌─────────────────────┐
                   │ Application Gateway │
                   │   + WAF v2          │
                   │   (TLS Termination) │
                   └──────────┬──────────┘
                              │
                ┌─────────────┴─────────────┐
                │                           │
                ▼                           ▼
        ┌──────────────┐           ┌──────────────┐
        │   Ingress    │           │  API Mgmt    │
        │  Controller  │           │   (APIM)     │
        └──────┬───────┘           └──────┬───────┘
               │                          │
      ┌────────┴─────────┐                │
      │                  │                │
      ▼                  ▼                ▼
┌──────────┐      ┌──────────┐    ┌──────────┐
│ Frontend │      │ Backend  │    │ Backend  │
│  React   │─────▶│  Spring  │    │  Spring  │
│   SPA    │      │   Boot   │    │   Boot   │
│ (nginx)  │      │  (REST)  │    │  (REST)  │
└──────────┘      └─────┬────┘    └─────┬────┘
                        │               │
                        ▼               ▼
                  ┌──────────────────────┐
                  │   Azure Key Vault    │
                  │   (Secrets + Certs)  │
                  └──────────────────────┘
                             │
                             ▼
                  ┌──────────────────────┐
                  │   Azure Monitor      │
                  │   + Log Analytics    │
                  │   + App Insights     │
                  └──────────────────────┘
```

## Components

### 1. Frontend Layer

**Technology**: React 18 + MSAL.js

**Features**:
- Single Page Application (SPA)
- Azure Entra (Azure AD) SSO authentication
- Responsive UI
- Client-side routing
- Served via nginx in production

**Container**:
- Multi-stage Docker build
- nginx:alpine base image
- Security headers configured
- Health endpoint: `/health`

**Deployment**:
- Deployed to AKS via Helm
- Horizontal Pod Autoscaler (HPA)
- Pod Disruption Budget (PDB)
- Resource limits and requests

### 2. Backend Layer

**Technology**: Spring Boot 3.x + Java 17

**Features**:
- RESTful API architecture
- OAuth2 Resource Server (JWT validation)
- Layered architecture:
  - **Controller**: REST endpoints
  - **Service**: Business logic
  - **Repository**: Data access
  - **DTO**: Data transfer objects
- OpenAPI/Swagger documentation
- Resilience4j patterns:
  - Circuit breaker
  - Rate limiting
  - Retry logic
  - Fallback handlers

**Endpoints**:
- `/api/hello`: Sample authenticated endpoint
- `/actuator/health`: Health check
- `/actuator/health/liveness`: Liveness probe
- `/actuator/health/readiness`: Readiness probe
- `/actuator/metrics`: Prometheus metrics
- `/swagger-ui.html`: API documentation

**Container**:
- Multi-stage build with Maven
- Eclipse Temurin JRE 17
- Non-root user execution
- Health checks configured

### 3. Infrastructure Layer

#### Azure Kubernetes Service (AKS)

**Configuration**:
- Kubernetes version: 1.27+
- Node pools with autoscaling
- System-assigned managed identity
- Azure CNI networking
- Azure Network Policy
- RBAC enabled
- Azure AD integration

**Features**:
- Cluster autoscaler
- Horizontal Pod Autoscaler
- Pod Disruption Budgets
- Resource quotas and limits
- Network policies
- Pod security standards

#### Azure Container Registry (ACR)

**Purpose**: Container image storage

**Features**:
- Geo-replication (Premium SKU)
- Security scanning integration
- Content trust
- Managed identity access from AKS

#### Azure Key Vault

**Purpose**: Secrets and certificate management

**Features**:
- Soft delete and purge protection
- RBAC authorization
- Private endpoint (optional)
- CSI driver integration with AKS
- Audit logging

**Stored Secrets**:
- Azure AD client IDs and secrets
- Database connection strings
- API keys
- TLS certificates

#### Application Gateway + WAF v2

**Purpose**: Web application firewall and load balancer

**Features**:
- TLS termination
- OWASP top 10 protection
- Custom WAF rules
- Path-based routing
- SSL policy configuration
- Auto-scaling

#### API Management (APIM)

**Purpose**: API gateway and management

**Features**:
- API versioning
- Rate limiting and quotas
- OAuth2 authentication
- Request/response transformation
- Analytics and monitoring
- Developer portal

#### Azure Monitor + Log Analytics

**Purpose**: Observability and monitoring

**Features**:
- Container insights
- Log aggregation
- Metrics collection
- Alert rules
- Workbooks and dashboards

**Collected Data**:
- Application logs
- Container logs
- Kubernetes events
- Performance metrics
- Custom metrics (Prometheus)

#### Application Insights

**Purpose**: APM and telemetry

**Features**:
- Request tracking
- Dependency tracking
- Exception tracking
- Performance counters
- Custom events and metrics

## Security Architecture

### Defense in Depth

1. **Network Security**:
   - Azure Front Door with DDoS protection
   - Application Gateway WAF v2
   - Network Security Groups (NSGs)
   - Azure CNI with Network Policies
   - Private endpoints for PaaS services

2. **Identity & Access**:
   - Azure AD authentication (MSAL)
   - Managed identities for Azure resources
   - RBAC for AKS and Azure resources
   - Just-in-time (JIT) access
   - Least privilege principle

3. **Data Protection**:
   - TLS 1.2+ for all communications
   - Secrets stored in Key Vault
   - Encryption at rest (Azure managed keys)
   - Encryption in transit
   - No secrets in code or config files

4. **Container Security**:
   - Image scanning with Trivy
   - Non-root container execution
   - Read-only root filesystem where possible
   - Resource limits and quotas
   - Pod security standards

5. **Application Security**:
   - OAuth2/JWT authentication
   - CORS policy enforcement
   - Security headers (HSTS, CSP, etc.)
   - Input validation
   - SQL injection prevention
   - XSS protection

## CI/CD Architecture

### Build Pipelines

**Frontend Build**:
1. Install dependencies (`npm ci`)
2. Run linter
3. Run unit tests (Jest)
4. Code coverage analysis
5. SonarQube scan
6. Build Docker image
7. Trivy security scan
8. Push to ACR

**Backend Build**:
1. Maven build
2. Run unit tests (JUnit)
3. Code coverage (JaCoCo)
4. SonarQube analysis
5. Run integration tests (Cucumber)
6. Build Docker image
7. Trivy security scan
8. Push to ACR

### Infrastructure Pipeline

1. Terraform validation
2. tflint checks
3. tfsec security scan
4. Terraform plan
5. Manual approval (for prod)
6. Terraform apply
7. Output infrastructure details

### Release Pipeline

1. Deploy to AKS with Helm
2. Health check validation
3. Integration tests
4. OWASP ZAP DAST scan
5. Performance tests (optional)
6. Approval gate for production
7. Blue-green or canary deployment (optional)

## Data Flow

### User Authentication Flow

```
1. User accesses Frontend
2. Frontend redirects to Azure AD (MSAL)
3. User authenticates with Azure AD
4. Azure AD returns ID token and access token
5. Frontend stores tokens in session
6. Frontend calls Backend API with Bearer token
7. Backend validates JWT token with Azure AD
8. Backend processes request and returns response
```

### API Request Flow

```
1. User → Frontend SPA
2. Frontend → Application Gateway → Ingress → Frontend Pod
3. Frontend (with token) → Application Gateway → APIM → Backend Pod
4. Backend validates token
5. Backend processes business logic
6. Backend queries database/external services (with resilience)
7. Response flows back through the chain
```

## Resilience Patterns

### Circuit Breaker
- Prevents cascading failures
- Opens after threshold failures
- Half-open state for recovery testing
- Configured in Resilience4j

### Retry Logic
- Exponential backoff
- Maximum retry attempts
- Retry on specific exceptions
- Prevents overwhelming failed services

### Rate Limiting
- API-level rate limiting in APIM
- Application-level in Resilience4j
- Kubernetes resource quotas
- DDoS protection at Front Door/WAF

### Timeouts
- Connection timeouts
- Read timeouts
- Request timeouts
- Configured at multiple layers

## Scalability

### Horizontal Scaling
- HPA based on CPU/memory
- Cluster autoscaler for nodes
- Scale-to-zero for dev environments
- Min/max replica configuration

### Performance Optimization
- CDN for static assets (Front Door)
- Image optimization
- Database connection pooling
- Caching strategies
- Lazy loading in frontend

## Disaster Recovery

### Backup Strategy
- Infrastructure as Code (Terraform)
- GitOps for configurations
- ACR geo-replication
- Key Vault backups
- Database backups (if applicable)

### Recovery Procedures
1. Restore infrastructure from Terraform
2. Pull images from replicated ACR
3. Deploy applications via Helm
4. Restore secrets from backup
5. Validate functionality

### High Availability
- Multi-zone node pools
- Pod anti-affinity rules
- Application Gateway redundancy
- APIM multi-region (Premium)
- Database replication (if applicable)

## Monitoring & Observability

### Metrics
- Infrastructure metrics (CPU, memory, disk)
- Application metrics (request rate, latency, errors)
- Business metrics (user activity, transactions)
- Custom Prometheus metrics

### Logs
- Application logs (structured JSON)
- Container logs
- Kubernetes audit logs
- Access logs (Application Gateway, APIM)

### Alerts
- Service health alerts
- Performance degradation
- Error rate thresholds
- Resource exhaustion
- Security incidents

### Dashboards
- Azure Monitor workbooks
- Grafana dashboards
- Application Insights dashboards
- Custom dashboards

## Cost Optimization

### Strategies
1. Right-size VM SKUs
2. Use spot instances for non-prod
3. Auto-scale down during off-hours
4. Reserved instances for prod
5. Monitor and optimize storage
6. Use appropriate ACR SKU per environment
7. Leverage AKS free tier
8. Cleanup unused resources

### Cost Breakdown (Estimated Monthly - Dev)
- AKS: $150-300
- Application Gateway: $150-200
- APIM: $50-100 (Developer tier)
- ACR: $5-20
- Key Vault: $1-5
- Log Analytics: $20-50
- **Total**: ~$400-700/month

## Compliance & Governance

### Azure Policy
- Enforce naming conventions
- Require tags on resources
- Restrict resource types
- Enforce encryption
- Network restrictions

### Regulatory Compliance
- GDPR considerations
- Data residency controls
- Audit trail requirements
- Access logging

## Future Enhancements

1. **Service Mesh**: Implement Istio or Linkerd
2. **GitOps**: Flux or ArgoCD for deployment
3. **Chaos Engineering**: Chaos Mesh for resilience testing
4. **Advanced Networking**: Azure Private Link, ExpressRoute
5. **Multi-Region**: Active-active deployment
6. **Database**: Add managed database (PostgreSQL/MySQL)
7. **Caching**: Redis cache layer
8. **Message Queue**: Azure Service Bus or Event Hub
9. **Background Jobs**: Kubernetes CronJobs or Azure Functions
10. **AI/ML**: Integrate Azure ML or Cognitive Services
