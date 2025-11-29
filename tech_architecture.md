# Technical Architecture

## Overview

This document describes the technical architecture of the AKS Starter Kit, a production-ready cloud-native application built on Azure Kubernetes Service (AKS). The architecture follows Azure Well-Architected Framework principles and implements enterprise-grade security, reliability, and operational excellence.

---

## High-Level Architecture

```mermaid
graph TB
    subgraph "User Layer"
        User[End User Browser]
    end

    subgraph "Azure Cloud"
        subgraph "Identity & Access"
            AAD[Azure Active Directory]
        end

        subgraph "CI/CD Pipeline"
            GHA[GitHub Actions]
            ACR[Azure Container Registry]
        end

        subgraph "Azure Kubernetes Service"
            subgraph "Ingress Layer"
                ALB[Azure Load Balancer]
                NGINX[NGINX Ingress Controller]
                CM[cert-manager]
            end

            subgraph "Application Layer"
                FE[Frontend Pod<br/>React SPA]
                BE[Backend Pod<br/>Spring Boot API]
            end

            subgraph "Data Layer"
                H2[H2 Database<br/>In-Memory]
            end
        end

        subgraph "Monitoring & Observability"
            ACT[Actuator Endpoints]
            PROM[Prometheus Metrics]
        end
    end

    User -->|HTTPS| ALB
    ALB -->|TLS Termination| NGINX
    NGINX -->|Route /| FE
    NGINX -->|Route /api/*| BE

    FE -->|OAuth2 PKCE| AAD
    BE -->|JWT Validation| AAD
    FE -->|API Calls + Bearer Token| BE
    BE --> H2

    GHA -->|Build & Push Images| ACR
    GHA -->|Deploy Helm Charts| NGINX
    GHA -->|Deploy Helm Charts| FE
    GHA -->|Deploy Helm Charts| BE

    BE --> ACT
    ACT --> PROM
    CM -->|Issue TLS Certs| NGINX

    style AAD fill:#0078D4
    style AKS fill:#326CE5
    style ACR fill:#0078D4
    style ALB fill:#0078D4
```

---

## Detailed Application Flow

```mermaid
sequenceDiagram
    participant User
    participant ALB as Azure Load Balancer
    participant NGINX as NGINX Ingress
    participant Frontend as React SPA
    participant AAD as Azure AD
    participant Backend as Spring Boot API
    participant DB as H2 Database

    User->>ALB: HTTPS Request (IP-based)
    ALB->>NGINX: Forward Request + TLS Termination

    alt Frontend Request
        NGINX->>Frontend: Route / to Frontend Pod
        Frontend->>User: Serve React App
        User->>AAD: OAuth2 Authorization Code Flow (PKCE)
        AAD->>User: Access Token + ID Token
        User->>Frontend: Token stored in browser
    end

    alt API Request
        User->>NGINX: API Call + Bearer Token
        NGINX->>Backend: Route /api/* to Backend Pod
        Backend->>AAD: Validate JWT Token
        AAD->>Backend: Token Valid + Claims
        Backend->>Backend: Check Authorization
        Backend->>DB: Query Data
        DB->>Backend: Return Data
        Backend->>User: API Response (JSON)
    end

    alt Health Check
        NGINX->>Backend: GET /actuator/health
        Backend->>NGINX: 200 OK + Health Status
    end
```

---

## CI/CD Pipeline Architecture

```mermaid
graph LR
    subgraph "Source Control"
        GH[GitHub Repository<br/>main/develop branches]
    end

    subgraph "Build Pipeline"
        BFE[Frontend Build<br/>npm test + build]
        BBE[Backend Build<br/>mvn test + package]
        SCAN[Security Scan<br/>Trivy + CodeQL]
    end

    subgraph "Artifact Storage"
        ACR[Azure Container Registry<br/>Docker Images]
    end

    subgraph "Deployment Pipeline"
        DEV[Deploy to Dev<br/>develop branch]
        STAGE[Deploy to Stage<br/>manual approval]
        PROD[Deploy to Prod<br/>manual approval]
    end

    subgraph "Quality Gates"
        QG1[Code Coverage ≥ 40%]
        QG2[Security Scan Pass]
        QG3[Code Style Check]
        QG4[Unit Tests Pass]
    end

    GH -->|Push/PR| BFE
    GH -->|Push/PR| BBE
    BFE --> QG1
    BFE --> QG3
    BFE --> QG4
    BBE --> QG1
    BBE --> QG3
    BBE --> QG4
    QG1 --> SCAN
    QG3 --> SCAN
    QG4 --> SCAN
    SCAN --> QG2
    QG2 --> ACR
    ACR --> DEV
    DEV --> STAGE
    STAGE --> PROD

    style QG1 fill:#28a745
    style QG2 fill:#28a745
    style QG3 fill:#28a745
    style QG4 fill:#28a745
```

---

## Security Layers

### 1. **Identity & Access Management**
- **Azure Active Directory (AAD)** integration for authentication
- **OAuth 2.0 Authorization Code Flow with PKCE** for frontend
- **JWT Bearer Token validation** for backend APIs
- **Role-Based Access Control (RBAC)** via AAD groups and claims

### 2. **Network Security**
- **Azure Load Balancer** with public IP for external access
- **NGINX Ingress Controller** for L7 routing and TLS termination
- **TLS 1.2+** encryption with self-signed certificates (dev) or Let's Encrypt (prod)
- **IP-based access** with optional IP whitelisting capabilities
- **Network Policies** (can be enabled) for pod-to-pod communication

### 3. **Application Security**
- **JWT token validation** on every API request
- **OAuth2 Resource Server** configuration with audience validation
- **CORS policies** configured per environment
- **Security headers** via NGINX annotations
- **Secrets management** via Kubernetes Secrets and Azure Key Vault (optional)

### 4. **Container Security**
- **Trivy vulnerability scanning** on every Docker image build
- **Non-root container execution** with security contexts
- **Read-only root filesystem** where applicable
- **Resource limits** to prevent resource exhaustion attacks
- **Image signing** via ACR (can be enabled)

### 5. **Code Security**
- **GitHub CodeQL** static analysis on every PR
- **Dependency scanning** via npm audit and Maven dependency-check
- **Security-focused code reviews** enforced via branch protection
- **OWASP Top 10** awareness in development practices
- **No hardcoded secrets** - all credentials via GitHub Secrets

### 6. **Pipeline Security**
- **Branch protection rules** on main/develop branches
- **Required PR reviews** before merge
- **Status checks** must pass before deployment
- **Environment approvals** for stage/prod deployments
- **Audit logs** via GitHub Actions history

---

## Code Quality Governance

```mermaid
graph TD
    DEV[Developer] -->|Commit Code| PR[Pull Request]
    PR --> LINT[Linting Checks]
    PR --> STYLE[Code Style Checks]
    PR --> TEST[Unit Tests]
    PR --> COV[Coverage Analysis]

    LINT -->|Frontend| ESLINT[ESLint]
    LINT -->|Backend| CHECKSTYLE[Checkstyle]

    STYLE -->|Frontend| PRETTIER[Prettier - Manual]
    STYLE -->|Backend| SPOTLESS[Spotless Maven]

    TEST -->|Frontend| JEST[Jest + React Testing Library]
    TEST -->|Backend| JUNIT[JUnit + Mockito]

    COV -->|Threshold| MIN[Min 40% Coverage]

    ESLINT --> GATE{Quality Gate}
    CHECKSTYLE --> GATE
    SPOTLESS --> GATE
    JEST --> GATE
    JUNIT --> GATE
    MIN --> GATE

    GATE -->|Pass| MERGE[Merge Allowed]
    GATE -->|Fail| REJECT[Merge Blocked]

    MERGE --> BUILD[CI Build]
    BUILD --> SCAN[Security Scan]
    SCAN --> DEPLOY[Deploy to Environment]

    style GATE fill:#ffc107
    style MERGE fill:#28a745
    style REJECT fill:#dc3545
```

### Quality Standards

#### **Frontend (React + TypeScript)**
- **ESLint**: JavaScript/TypeScript linting with React rules
- **Code Coverage**: Minimum 40% line coverage (configurable to 70%)
- **Testing**: Jest + React Testing Library
- **Build Validation**: Production build must succeed
- **Type Safety**: TypeScript strict mode (can be enabled)

#### **Backend (Spring Boot + Java)**
- **Checkstyle**: Google Java Style Guide enforcement
- **Spotless**: Automatic code formatting with fail-on-error
- **Code Coverage**: Minimum 40% across lines, branches, functions
- **Testing**: JUnit 5 + Mockito + Spring Boot Test
- **Static Analysis**: SonarQube integration ready

#### **Docker Images**
- **Multi-stage builds** for minimal image size
- **Trivy scanning** for vulnerabilities (CRITICAL + HIGH)
- **Layer caching** for faster builds
- **No unnecessary packages** in production images

---

## Application Resilience

```mermaid
graph TB
    subgraph "Client Layer"
        CLIENT[Client Request]
    end

    subgraph "Resilience Patterns"
        RLM[Rate Limiter<br/>100 req/sec]
        CB[Circuit Breaker<br/>50% failure threshold]
        RETRY[Retry Logic<br/>Max 3 attempts]
    end

    subgraph "Kubernetes Resilience"
        HPA[Horizontal Pod Autoscaler<br/>CPU-based scaling]
        HEALTH[Health Probes<br/>Liveness + Readiness]
        ROLLOUT[Rolling Updates<br/>Zero-downtime]
    end

    subgraph "Application Layer"
        BE[Backend Service]
        EXT[External Service]
    end

    CLIENT --> RLM
    RLM --> CB
    CB --> RETRY
    RETRY --> BE
    BE --> EXT

    HPA -.->|Scales| BE
    HEALTH -.->|Monitors| BE
    ROLLOUT -.->|Updates| BE

    style CB fill:#ff6b6b
    style RLM fill:#4ecdc4
    style RETRY fill:#95e1d3
    style HPA fill:#f38181
    style HEALTH fill:#aa96da
```

### Resilience Mechanisms

#### **1. Circuit Breaker (Resilience4j)**
```yaml
Configuration:
  - Sliding Window Size: 10 requests
  - Failure Rate Threshold: 50%
  - Wait Duration in Open State: 10 seconds
  - Half-Open Permitted Calls: 3
```

#### **2. Retry Policy**
```yaml
Configuration:
  - Max Attempts: 3
  - Wait Duration: 1 second
  - Retry Exceptions: IOException, TimeoutException
```

#### **3. Rate Limiting**
```yaml
Configuration:
  - Limit per Period: 100 requests
  - Period: 1 second
  - Timeout: Fail immediately
```

#### **4. Kubernetes Health Checks**
- **Liveness Probe**: Detects if pod is alive (restart if failing)
- **Readiness Probe**: Detects if pod is ready for traffic
- **Startup Probe**: Allows slow-starting containers

#### **5. Horizontal Pod Autoscaling**
```yaml
Dev Environment:
  - Min Replicas: 2
  - Max Replicas: 5
  - Target CPU: 70%

Production Environment:
  - Min Replicas: 3
  - Max Replicas: 15
  - Target CPU: 70%
```

#### **6. Rolling Updates**
```yaml
Strategy:
  - Max Unavailable: 0
  - Max Surge: 1
  - Ensures zero-downtime deployments
```

---

## Azure Well-Architected Framework Alignment

### 🎯 **1. Reliability**

| Principle | Implementation |
|-----------|----------------|
| **Design for failure** | Circuit breakers, retries, fallback mechanisms |
| **High availability** | Multi-replica deployments (2-15 pods) |
| **Self-healing** | Kubernetes liveness/readiness probes with auto-restart |
| **Disaster recovery** | Infrastructure as Code (Helm charts) for quick recreation |
| **Health monitoring** | Actuator endpoints + Prometheus metrics |

### 🔒 **2. Security**

| Principle | Implementation |
|-----------|----------------|
| **Defense in depth** | 6-layer security model (see Security Layers) |
| **Least privilege** | RBAC for service accounts, minimal container permissions |
| **Identity-based access** | Azure AD OAuth2 authentication + JWT authorization |
| **Encryption** | TLS in transit, secrets encrypted at rest |
| **Vulnerability management** | Trivy scanning on every build, CodeQL analysis |

### 💰 **3. Cost Optimization**

| Principle | Implementation |
|-----------|----------------|
| **Right-sizing** | HPA scales down to min replicas during low traffic |
| **Resource limits** | CPU/memory limits prevent over-provisioning |
| **Efficient builds** | Docker layer caching reduces build time and costs |
| **Spot instances** | Can use Azure Spot VMs for non-prod (not configured) |
| **Environment-specific sizing** | Dev (2 pods) vs Prod (3-15 pods) |

### ⚡ **4. Performance Efficiency**

| Principle | Implementation |
|-----------|----------------|
| **Horizontal scaling** | HPA based on CPU metrics |
| **Caching** | Docker layer caching, Maven/npm dependency caching |
| **CDN** | Can integrate Azure CDN for static assets (not configured) |
| **Database optimization** | Connection pooling, query optimization |
| **Load balancing** | Azure LB + NGINX Ingress for traffic distribution |

### 🔧 **5. Operational Excellence**

| Principle | Implementation |
|-----------|----------------|
| **IaC** | Helm charts for declarative infrastructure |
| **CI/CD automation** | GitHub Actions with multi-stage pipelines |
| **Monitoring** | Actuator health endpoints, Prometheus metrics |
| **Automated testing** | Unit tests, coverage thresholds, security scans |
| **Rollback capability** | Helm rollback, immutable image tags |
| **Documentation** | Inline code docs, architecture diagrams, README files |

---

## Technology Stack

### **Frontend**
- **Framework**: React 18.2
- **Authentication**: @azure/msal-react 2.0
- **HTTP Client**: Axios 1.6
- **Routing**: React Router 6.20
- **Build Tool**: Create React App (react-scripts 5.0)
- **Testing**: Jest + React Testing Library
- **Linting**: ESLint

### **Backend**
- **Framework**: Spring Boot 3.2.1
- **Language**: Java 17
- **Authentication**: Spring Security OAuth2 Resource Server
- **Resilience**: Resilience4j (Circuit Breaker, Retry, Rate Limiter)
- **Database**: H2 (in-memory, production-ready options: PostgreSQL, MySQL)
- **Monitoring**: Spring Boot Actuator + Micrometer Prometheus
- **Build Tool**: Maven 3.9
- **Testing**: JUnit 5 + Mockito + Spring Boot Test

### **Infrastructure**
- **Container Orchestration**: Azure Kubernetes Service (AKS)
- **Ingress**: NGINX Ingress Controller
- **TLS Management**: cert-manager with self-signed certificates
- **Load Balancer**: Azure Load Balancer
- **Container Registry**: Azure Container Registry (ACR)
- **Package Manager**: Helm 3.13

### **CI/CD**
- **Pipeline**: GitHub Actions
- **Security Scanning**: Trivy (container), CodeQL (code)
- **Deployment Strategy**: Rolling updates with Helm

### **Observability**
- **Metrics**: Prometheus
- **Health Checks**: Spring Boot Actuator
- **Logging**: Console logs (can integrate Azure Monitor)

---

## Deployment Architecture by Environment

### **Development (dev)**
```yaml
Frontend:
  - Replicas: 2
  - Resources: Default (requests/limits not set)
  - Autoscaling: Disabled

Backend:
  - Replicas: 2
  - Resources: Default
  - Autoscaling: Disabled

Ingress:
  - TLS: Self-signed certificates
  - Access: IP-based (no domain required)

Deployment Trigger:
  - Automatic on push to develop branch
  - After both build workflows pass
```

### **Staging (stage)**
```yaml
Frontend:
  - Replicas: 3
  - Resources: Default
  - Autoscaling: Disabled

Backend:
  - Replicas: 3
  - Resources: Default
  - Autoscaling: Disabled

Ingress:
  - TLS: Self-signed or Let's Encrypt
  - Access: Domain-based or IP-based

Deployment Trigger:
  - Manual via workflow_dispatch
  - Requires dev deployment success
```

### **Production (prod)**
```yaml
Frontend:
  - Replicas: 5 (initial)
  - Autoscaling: 3-10 pods based on CPU
  - Resources:
      Requests: 256Mi memory, 250m CPU
      Limits: 512Mi memory, 500m CPU

Backend:
  - Replicas: 5 (initial)
  - Autoscaling: 3-15 pods based on CPU
  - Resources:
      Requests: 512Mi memory, 500m CPU
      Limits: 1Gi memory, 1000m CPU

Ingress:
  - TLS: Let's Encrypt certificates
  - Access: Domain-based with CDN

Deployment Trigger:
  - Manual via workflow_dispatch
  - Requires stage deployment success
  - Environment protection rules
```

---

## Data Flow & Security

```mermaid
graph LR
    subgraph "External"
        U[User Browser]
    end

    subgraph "Azure LB + NGINX"
        LB[Load Balancer<br/>Public IP]
        NG[NGINX Ingress<br/>TLS Termination]
    end

    subgraph "Frontend Pod"
        FE[React App<br/>No sensitive data]
    end

    subgraph "Backend Pod"
        BE[Spring Boot API<br/>JWT Validation]
    end

    subgraph "Data Store"
        DB[(H2 Database<br/>In-Memory)]
    end

    subgraph "Identity Provider"
        AD[Azure Active Directory<br/>Token Issuer]
    end

    U -->|1. HTTPS Request| LB
    LB -->|2. Forward| NG
    NG -->|3. Route| FE
    FE -->|4. Login Request| AD
    AD -->|5. Access Token| U
    U -->|6. API Call + Token| NG
    NG -->|7. Route| BE
    BE -->|8. Validate Token| AD
    BE -->|9. Query Data| DB
    DB -->|10. Return Data| BE
    BE -->|11. JSON Response| U

    style AD fill:#0078D4
    style DB fill:#326CE5
    style LB fill:#0078D4
```

### Security Controls per Layer

| Layer | Security Controls |
|-------|-------------------|
| **Load Balancer** | DDoS protection, Public IP with firewall rules |
| **Ingress** | TLS termination, Rate limiting, IP filtering (optional) |
| **Frontend** | CSP headers, XSS protection, no sensitive data storage |
| **Backend** | JWT validation, Input validation, SQL injection prevention |
| **Database** | In-memory (dev), Encrypted at rest (prod), Access controls |
| **Secrets** | Kubernetes secrets, Azure Key Vault integration (optional) |

---

## Monitoring & Observability

### **Application Health**
```yaml
Endpoints:
  - /actuator/health: Overall health status
  - /actuator/health/liveness: Liveness probe
  - /actuator/health/readiness: Readiness probe
  - /actuator/info: Application metadata
  - /actuator/metrics: Prometheus metrics
```

### **Key Metrics Tracked**
- **Request Rate**: Requests per second per endpoint
- **Error Rate**: 4xx and 5xx responses
- **Response Time**: P50, P95, P99 latencies
- **Resource Usage**: CPU, memory per pod
- **Circuit Breaker State**: Open/closed/half-open
- **Database Connections**: Pool size and active connections

### **Health Probe Configuration**
```yaml
Liveness:
  - Path: /actuator/health/liveness
  - Initial Delay: 30s
  - Period: 10s
  - Timeout: 5s

Readiness:
  - Path: /actuator/health/readiness
  - Initial Delay: 10s
  - Period: 5s
  - Timeout: 3s
```

---

## Future Enhancements

### **Security**
- [ ] Azure Key Vault integration for secrets management
- [ ] Azure Policy enforcement for compliance
- [ ] Network policies for pod-to-pod communication
- [ ] Web Application Firewall (WAF) via Azure Application Gateway

### **Observability**
- [ ] Azure Monitor / Application Insights integration
- [ ] Distributed tracing with OpenTelemetry
- [ ] Centralized logging with Azure Log Analytics
- [ ] Custom dashboards for business metrics

### **Performance**
- [ ] Azure CDN for static asset delivery
- [ ] Redis cache for session management
- [ ] Azure Database for PostgreSQL (replace H2)
- [ ] Connection pooling optimization

### **Reliability**
- [ ] Multi-region deployment for disaster recovery
- [ ] Azure Front Door for global load balancing
- [ ] Automated backup and restore procedures
- [ ] Chaos engineering with Azure Chaos Studio

### **Cost Optimization**
- [ ] Azure Spot VMs for non-production environments
- [ ] Reserved instances for predictable workloads
- [ ] Cost analysis and optimization recommendations
- [ ] Automated scaling policies based on business metrics

---

## Conclusion

This architecture demonstrates a production-ready, cloud-native application that:

✅ **Scales horizontally** based on demand (2-15 pods)
✅ **Secures at every layer** with Azure AD, TLS, and container scanning
✅ **Maintains high availability** through health checks and auto-healing
✅ **Enforces code quality** with automated testing and coverage thresholds
✅ **Deploys safely** with rolling updates and environment protection
✅ **Monitors proactively** with health endpoints and metrics
✅ **Follows Azure best practices** aligned with Well-Architected Framework

The architecture is designed to be **extensible**, **maintainable**, and **production-ready** from day one.
