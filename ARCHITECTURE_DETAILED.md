# AKS Starter Kit - Detailed Architecture

## Complete Architecture Overview with Azure Services

This document provides a comprehensive architectural overview of the AKS Starter Kit, including all Azure services used, their purposes, and how they interact.

---

## Table of Contents

1. [High-Level Architecture](#high-level-architecture)
2. [Azure Services Used](#azure-services-used)
3. [Network Architecture](#network-architecture)
4. [Security Architecture](#security-architecture)
5. [Data Flow](#data-flow)
6. [Deployment Architecture](#deployment-architecture)
7. [Monitoring & Observability](#monitoring--observability)
8. [Disaster Recovery](#disaster-recovery)

---

## High-Level Architecture

```
┌───────────────────────────────────────────────────────────────────────────────────┐
│                                    USERS                                          │
│                                                                                   │
│                 Internet Users         Corporate Users (VPN)                      │
│                       │                          │                                │
└───────────────────────┼──────────────────────────┼────────────────────────────────┘
                        │                          │
                        └──────────┬───────────────┘
                                   │
                                   ▼
┌───────────────────────────────────────────────────────────────────────────────────┐
│                            AZURE FRONT DOOR (Optional)                            │
│                                                                                   │
│  • Global load balancing                                                         │
│  • SSL/TLS termination                                                           │
│  • DDoS protection                                                               │
│  • CDN caching                                                                   │
└───────────────────────────────┬───────────────────────────────────────────────────┘
                                │
                                ▼
┌───────────────────────────────────────────────────────────────────────────────────┐
│                      AZURE APPLICATION GATEWAY (WAF v2)                           │
│                                                                                   │
│  • Web Application Firewall (WAF)                                                │
│  • SSL/TLS termination                                                           │
│  • Path-based routing                                                            │
│  • Session affinity                                                              │
│  • Health probes                                                                 │
│                                                                                   │
│  Public IP: X.X.X.X                                                              │
│  DNS: yourdomain.com → X.X.X.X                                                   │
│                                                                                   │
│  ┌─────────────────┐                    ┌─────────────────┐                     │
│  │  Frontend Rules │                    │  Backend Pools   │                     │
│  │                 │                    │                  │                     │
│  │  Path: /        │────────────────────│  AKS Frontend   │                     │
│  │  Path: /api/*   │────────────────────│  AKS Backend    │                     │
│  └─────────────────┘                    └─────────────────┘                     │
└───────────────────────────────┬───────────────────────────────────────────────────┘
                                │
                                ▼
┌───────────────────────────────────────────────────────────────────────────────────┐
│                      AZURE API MANAGEMENT (APIM)                                  │
│                                                                                   │
│  • API Gateway                                                                   │
│  • Rate limiting & throttling                                                    │
│  • API versioning                                                                │
│  • OAuth2 validation                                                             │
│  • Request/response transformation                                               │
│  • Developer portal                                                              │
│  • Analytics & monitoring                                                        │
└───────────────────────────────┬───────────────────────────────────────────────────┘
                                │
                                ▼
┌───────────────────────────────────────────────────────────────────────────────────┐
│                    AZURE KUBERNETES SERVICE (AKS)                                 │
│                                                                                   │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │                         NAMESPACE: prod                                  │    │
│  │                                                                          │    │
│  │  ┌──────────────────────────────────────────────────────────────┐       │    │
│  │  │                    FRONTEND (React SPA)                       │       │    │
│  │  │                                                               │       │    │
│  │  │  ┌────────────┐  ┌────────────┐  ┌────────────┐             │       │    │
│  │  │  │   Pod 1    │  │   Pod 2    │  │   Pod 3    │   (HPA)     │       │    │
│  │  │  │            │  │            │  │            │             │       │    │
│  │  │  │  nginx     │  │  nginx     │  │  nginx     │             │       │    │
│  │  │  │  React App │  │  React App │  │  React App │             │       │    │
│  │  │  └────────────┘  └────────────┘  └────────────┘             │       │    │
│  │  │                                                               │       │    │
│  │  │  Service: frontend-svc (ClusterIP)                           │       │    │
│  │  │  Ingress: yourdomain.com                                     │       │    │
│  │  └──────────────────────────────────────────────────────────────┘       │    │
│  │                                                                          │    │
│  │  ┌──────────────────────────────────────────────────────────────┐       │    │
│  │  │                  BACKEND (Spring Boot API)                    │       │    │
│  │  │                                                               │       │    │
│  │  │  ┌────────────┐  ┌────────────┐  ┌────────────┐             │       │    │
│  │  │  │   Pod 1    │  │   Pod 2    │  │   Pod 3    │   (HPA)     │       │    │
│  │  │  │            │  │            │  │            │             │       │    │
│  │  │  │ Spring     │  │ Spring     │  │ Spring     │             │       │    │
│  │  │  │ Boot 3.x   │  │ Boot 3.x   │  │ Boot 3.x   │             │       │    │
│  │  │  │ Java 17    │  │ Java 17    │  │ Java 17    │             │       │    │
│  │  │  └────────────┘  └────────────┘  └────────────┘             │       │    │
│  │  │                                                               │       │    │
│  │  │  Service: backend-svc (ClusterIP)                            │       │    │
│  │  │  Ingress: api.yourdomain.com                                 │       │    │
│  │  └──────────────────────────────────────────────────────────────┘       │    │
│  │                                                                          │    │
│  │  ┌──────────────────────────────────────────────────────────────┐       │    │
│  │  │                   INFRASTRUCTURE PODS                         │       │    │
│  │  │                                                               │       │    │
│  │  │  • cert-manager (TLS certificate management)                 │       │    │
│  │  │  • ingress-nginx (Ingress controller)                        │       │    │
│  │  │  • prometheus (Metrics collection)                           │       │    │
│  │  │  • kube-state-metrics                                         │       │    │
│  │  │  • secrets-store-csi-driver (Key Vault integration)          │       │    │
│  │  └──────────────────────────────────────────────────────────────┘       │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
│                                                                                   │
│  Node Pools:                                                                     │
│    • System Pool: 2-3 nodes (Standard_D2s_v3)                                   │
│    • User Pool: 3-15 nodes (Standard_D4s_v3) - Autoscaling enabled             │
│                                                                                   │
│  Network: Azure CNI, Calico Network Policy                                      │
│  Identity: Managed Identity (for Azure resource access)                         │
└───────────────────────────────┬───────────────────────────────────────────────────┘
                                │
                                ├─────────────────────────────┐
                                │                             │
                                ▼                             ▼
┌────────────────────────────────────────┐   ┌────────────────────────────────────┐
│   AZURE CONTAINER REGISTRY (ACR)       │   │   AZURE KEY VAULT                  │
│                                        │   │                                    │
│  • Store Docker images                 │   │  • Store secrets                   │
│  • Geo-replication (optional)          │   │    - Azure AD client secrets       │
│  • Security scanning                   │   │    - Database passwords            │
│  • Signed images                       │   │    - API keys                      │
│  • Private endpoint (optional)         │   │    - TLS certificates              │
│                                        │   │  • Managed by Terraform            │
│  Repositories:                         │   │  • Access via Managed Identity     │
│    - aks-starter-frontend              │   │  • Audit logging enabled           │
│    - aks-starter-backend               │   │                                    │
└────────────────────────────────────────┘   └────────────────────────────────────┘
                                │
                                ▼
┌────────────────────────────────────────────────────────────────────────────────────┐
│                         AZURE ENTRA ID (Azure AD)                                  │
│                                                                                    │
│  ┌──────────────────────┐                    ┌──────────────────────┐            │
│  │  Frontend App Reg    │                    │  Backend App Reg     │            │
│  │                      │                    │                      │            │
│  │  Client ID: xxx      │──────API Calls────▶│  Client ID: yyy      │            │
│  │  Redirect URIs       │    (Bearer Token)  │  API Scopes:         │            │
│  │  • localhost:3000    │                    │  • access_as_user    │            │
│  │  • yourdomain.com    │                    │                      │            │
│  └──────────────────────┘                    └──────────────────────┘            │
│                                                                                    │
│  Authentication Flow:                                                             │
│    1. User logs in via MSAL.js (frontend)                                        │
│    2. Azure AD issues access token                                               │
│    3. Frontend sends token to backend                                            │
│    4. Backend validates token (OAuth2 Resource Server)                           │
│    5. Backend returns protected resource                                         │
└────────────────────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌────────────────────────────────────────────────────────────────────────────────────┐
│                        MONITORING & OBSERVABILITY                                  │
│                                                                                    │
│  ┌────────────────────────────────────────────────────────────────────────┐      │
│  │                    LOG ANALYTICS WORKSPACE                              │      │
│  │                                                                         │      │
│  │  • Centralized logging                                                  │      │
│  │  • KQL queries                                                          │      │
│  │  • Log retention (30-90 days)                                           │      │
│  │  • Integration with all Azure resources                                │      │
│  │                                                                         │      │
│  │  Data Sources:                                                          │      │
│  │    ✓ AKS container logs                                                 │      │
│  │    ✓ Application Gateway logs                                           │      │
│  │    ✓ API Management logs                                                │      │
│  │    ✓ Key Vault audit logs                                               │      │
│  │    ✓ Activity logs (Azure resource changes)                             │      │
│  └────────────────────────────────────────────────────────────────────────┘      │
│                                                                                    │
│  ┌────────────────────────────────────────────────────────────────────────┐      │
│  │                    APPLICATION INSIGHTS                                 │      │
│  │                                                                         │      │
│  │  • Application performance monitoring (APM)                             │      │
│  │  • Distributed tracing                                                  │      │
│  │  • Dependency tracking                                                  │      │
│  │  • Custom events and metrics                                            │      │
│  │  • Live metrics stream                                                  │      │
│  │  • Failure analytics                                                    │      │
│  │                                                                         │      │
│  │  Instrumentation:                                                       │      │
│  │    • Frontend: MSAL telemetry, custom events                            │      │
│  │    • Backend: Spring Boot auto-instrumentation                          │      │
│  └────────────────────────────────────────────────────────────────────────┘      │
│                                                                                    │
│  ┌────────────────────────────────────────────────────────────────────────┐      │
│  │                    AZURE MONITOR                                        │      │
│  │                                                                         │      │
│  │  • Metrics collection (CPU, memory, network)                            │      │
│  │  • Alert rules                                                          │      │
│  │  • Action groups (email, SMS, webhooks)                                 │      │
│  │  • Workbooks (custom dashboards)                                        │      │
│  │  • Autoscale rules                                                      │      │
│  └────────────────────────────────────────────────────────────────────────┘      │
│                                                                                    │
│  ┌────────────────────────────────────────────────────────────────────────┐      │
│  │                PROMETHEUS + GRAFANA (In AKS)                            │      │
│  │                                                                         │      │
│  │  • Kubernetes metrics                                                   │      │
│  │  • Pod metrics                                                          │      │
│  │  • Custom application metrics                                           │      │
│  │  • Grafana dashboards                                                   │      │
│  └────────────────────────────────────────────────────────────────────────┘      │
└────────────────────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌────────────────────────────────────────────────────────────────────────────────────┐
│                            CI/CD PIPELINE                                          │
│                                                                                    │
│  Azure DevOps Services:                                                           │
│                                                                                    │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐   │
│  │   Repos      │    │  Pipelines   │    │  Artifacts   │    │ Environments │   │
│  │              │    │              │    │              │    │              │   │
│  │  • Git repo  │───▶│  • Build     │───▶│  • Images    │───▶│  • Dev       │   │
│  │  • Branches  │    │  • Test      │    │  • Reports   │    │  • Stage     │   │
│  │  • PR checks │    │  • Scan      │    │  • Logs      │    │  • Prod      │   │
│  └──────────────┘    │  • Deploy    │    └──────────────┘    └──────────────┘   │
│                      │  • Release   │                                            │
│                      └──────────────┘                                            │
│                                                                                    │
│  External Integrations:                                                           │
│    • SonarQube (code quality)                                                     │
│    • Trivy (security scanning)                                                    │
│    • OWASP ZAP (dynamic security testing)                                         │
│    • JUnit/Jest (test results)                                                    │
└────────────────────────────────────────────────────────────────────────────────────┘
```

---

## Azure Services Used

### 1. Azure Kubernetes Service (AKS)

**Purpose**: Container orchestration platform for running microservices

**Key Features Used**:
- Managed Kubernetes control plane (free)
- Azure CNI networking for pod IP addresses
- Managed identities for Azure resource access
- Azure Active Directory integration for RBAC
- Cluster autoscaler for automatic node scaling
- Horizontal Pod Autoscaler (HPA) for pod scaling
- Azure Monitor integration for container insights

**Configuration**:
```hcl
# Terraform configuration
resource "azurerm_kubernetes_cluster" "aks" {
  name                = "aks-${var.project_name}-${var.environment}"
  location            = var.location
  resource_group_name = var.resource_group_name
  dns_prefix          = "${var.project_name}-${var.environment}"
  kubernetes_version  = var.kubernetes_version

  default_node_pool {
    name                = "system"
    node_count          = var.aks_node_count
    vm_size             = var.aks_node_size
    vnet_subnet_id      = var.aks_subnet_id
    enable_auto_scaling = true
    min_count           = 2
    max_count           = 5
  }

  identity {
    type = "SystemAssigned"
  }

  network_profile {
    network_plugin    = "azure"
    network_policy    = "calico"
    load_balancer_sku = "standard"
    outbound_type     = "loadBalancer"
  }

  azure_active_directory_role_based_access_control {
    managed            = true
    azure_rbac_enabled = true
  }

  oms_agent {
    log_analytics_workspace_id = var.log_analytics_workspace_id
  }
}
```

**Cost Considerations**:
- Control plane: Free
- Worker nodes: Pay per VM (e.g., Standard_D2s_v3: ~$70/month per node)
- Dev: ~$140/month (2 nodes)
- Prod: ~$210-1050/month (3-15 nodes with autoscaling)

**Why This Service**:
- Industry-standard container orchestration
- Managed control plane reduces operational overhead
- Native Azure integration
- Enterprise-grade security and compliance

---

### 2. Azure Container Registry (ACR)

**Purpose**: Private Docker registry for storing and managing container images

**Key Features Used**:
- Private container image storage
- Geo-replication (optional, for DR)
- Content trust (image signing)
- Security scanning with Azure Security Center
- Webhook notifications
- Service principal or managed identity authentication

**Configuration**:
```hcl
resource "azurerm_container_registry" "acr" {
  name                = "acr${var.project_name}${var.environment}"
  resource_group_name = var.resource_group_name
  location            = var.location
  sku                 = "Premium"  # Premium for geo-replication, VNet, etc.
  admin_enabled       = false      # Use managed identity instead

  georeplications = var.enable_geo_replication ? [
    {
      location = "westus"
      tags     = {}
    }
  ] : []

  network_rule_set = {
    default_action = "Deny"
    ip_rule = [
      {
        action   = "Allow"
        ip_range = var.allowed_ip_ranges
      }
    ]
    virtual_network = [
      {
        action    = "Allow"
        subnet_id = var.aks_subnet_id
      }
    ]
  }
}

# Grant AKS pull access to ACR
resource "azurerm_role_assignment" "aks_acr_pull" {
  scope                = azurerm_container_registry.acr.id
  role_definition_name = "AcrPull"
  principal_id         = azurerm_kubernetes_cluster.aks.kubelet_identity[0].object_id
}
```

**Cost Considerations**:
- Basic: ~$5/month (10 GB storage)
- Standard: ~$20/month (100 GB storage)
- Premium: ~$50/month (500 GB storage, geo-replication)

**Why This Service**:
- Secure private registry
- Close to AKS for fast pulls
- Native Azure integration
- No external dependencies (vs Docker Hub)

---

### 3. Azure Key Vault

**Purpose**: Securely store and manage secrets, keys, and certificates

**Key Features Used**:
- Secret management (API keys, connection strings, passwords)
- Certificate management (TLS/SSL certificates)
- Access policies for fine-grained permissions
- Audit logging for compliance
- Integration with AKS via CSI driver
- Soft delete and purge protection
- Azure Private Link (optional)

**Configuration**:
```hcl
resource "azurerm_key_vault" "kv" {
  name                        = "kv-${var.project_name}-${var.environment}-${random_id.kv_suffix.hex}"
  location                    = var.location
  resource_group_name         = var.resource_group_name
  enabled_for_disk_encryption = true
  tenant_id                   = data.azurerm_client_config.current.tenant_id
  soft_delete_retention_days  = 7
  purge_protection_enabled    = true
  sku_name                    = "standard"

  network_acls {
    bypass                     = "AzureServices"
    default_action             = "Deny"
    ip_rules                   = var.allowed_ip_ranges
    virtual_network_subnet_ids = [var.aks_subnet_id]
  }

  access_policy {
    tenant_id = data.azurerm_client_config.current.tenant_id
    object_id = azurerm_kubernetes_cluster.aks.kubelet_identity[0].object_id

    secret_permissions = [
      "Get",
      "List"
    ]

    certificate_permissions = [
      "Get",
      "List"
    ]
  }
}

# Store secrets
resource "azurerm_key_vault_secret" "backend_client_id" {
  name         = "backend-client-id"
  value        = var.backend_client_id
  key_vault_id = azurerm_key_vault.kv.id
}
```

**AKS Integration (CSI Driver)**:
```yaml
# SecretProviderClass for AKS
apiVersion: secrets-store.csi.x-k8s.io/v1
kind: SecretProviderClass
metadata:
  name: azure-keyvault
spec:
  provider: azure
  parameters:
    keyvaultName: "kv-aksstarter-prod-abc123"
    tenantId: "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
    objects: |
      array:
        - |
          objectName: backend-client-id
          objectType: secret
          objectVersion: ""
```

**Cost Considerations**:
- Standard tier: ~$0.03 per 10,000 transactions
- Typically <$5/month for this solution

**Why This Service**:
- Centralized secret management
- Eliminates hardcoded secrets
- Audit trail for compliance
- Automatic secret rotation (optional)

---

### 4. Application Gateway with WAF v2

**Purpose**: Layer 7 load balancer with Web Application Firewall

**Key Features Used**:
- SSL/TLS termination
- Web Application Firewall (WAF) with OWASP rules
- Path-based routing
- URL rewrite
- Health probes for backend pools
- Autoscaling
- End-to-end SSL (optional)
- Custom error pages
- Integration with Azure Monitor

**Configuration**:
```hcl
resource "azurerm_application_gateway" "appgw" {
  name                = "appgw-${var.project_name}-${var.environment}"
  resource_group_name = var.resource_group_name
  location            = var.location

  sku {
    name     = "WAF_v2"
    tier     = "WAF_v2"
    capacity = 2  # Autoscaling: min 2, max 10
  }

  autoscale_configuration {
    min_capacity = 2
    max_capacity = 10
  }

  gateway_ip_configuration {
    name      = "gateway-ip-config"
    subnet_id = var.appgw_subnet_id
  }

  frontend_port {
    name = "https-port"
    port = 443
  }

  frontend_ip_configuration {
    name                 = "frontend-ip"
    public_ip_address_id = azurerm_public_ip.appgw_pip.id
  }

  backend_address_pool {
    name = "backend-pool"
    ip_addresses = [
      var.aks_ingress_ip  # Kubernetes ingress controller IP
    ]
  }

  backend_http_settings {
    name                  = "backend-http-settings"
    cookie_based_affinity = "Disabled"
    port                  = 80
    protocol              = "Http"
    request_timeout       = 60
    probe_name            = "health-probe"
  }

  probe {
    name                = "health-probe"
    protocol            = "Http"
    path                = "/health"
    interval            = 30
    timeout             = 30
    unhealthy_threshold = 3
    host                = "127.0.0.1"
  }

  http_listener {
    name                           = "https-listener"
    frontend_ip_configuration_name = "frontend-ip"
    frontend_port_name             = "https-port"
    protocol                       = "Https"
    ssl_certificate_name           = "ssl-cert"
  }

  ssl_certificate {
    name     = "ssl-cert"
    data     = filebase64("path/to/certificate.pfx")
    password = var.ssl_certificate_password
  }

  request_routing_rule {
    name                       = "routing-rule"
    rule_type                  = "PathBasedRouting"
    http_listener_name         = "https-listener"
    backend_address_pool_name  = "backend-pool"
    backend_http_settings_name = "backend-http-settings"
    url_path_map_name          = "path-map"
  }

  url_path_map {
    name                               = "path-map"
    default_backend_address_pool_name  = "backend-pool"
    default_backend_http_settings_name = "backend-http-settings"

    path_rule {
      name                       = "api-rule"
      paths                      = ["/api/*"]
      backend_address_pool_name  = "backend-pool"
      backend_http_settings_name = "backend-http-settings"
    }
  }

  waf_configuration {
    enabled          = true
    firewall_mode    = "Prevention"  # Detection or Prevention
    rule_set_type    = "OWASP"
    rule_set_version = "3.2"

    disabled_rule_group = []  # Customize based on app needs
  }
}
```

**WAF Protection**:
- SQL injection protection
- Cross-site scripting (XSS) protection
- Command injection protection
- HTTP protocol violations
- Bot protection
- Rate limiting

**Cost Considerations**:
- WAF_v2 (2 capacity units): ~$250/month
- Additional per-GB data processing: ~$0.008/GB
- Estimated monthly cost: ~$300-400

**Why This Service**:
- Enterprise-grade security (WAF)
- Automatic threat protection
- SSL/TLS offloading reduces AKS load
- Central point for routing and security

---

### 5. Azure API Management (APIM)

**Purpose**: API gateway for managing, securing, and publishing APIs

**Key Features Used**:
- API gateway (reverse proxy)
- Rate limiting and quotas
- OAuth2 token validation
- API versioning
- Request/response transformation
- Developer portal
- API analytics
- Caching
- IP filtering
- Mock responses

**Configuration**:
```hcl
resource "azurerm_api_management" "apim" {
  name                = "apim-${var.project_name}-${var.environment}"
  location            = var.location
  resource_group_name = var.resource_group_name
  publisher_name      = var.publisher_name
  publisher_email     = var.publisher_email
  sku_name            = "Developer_1"  # Developer, Basic, Standard, Premium

  identity {
    type = "SystemAssigned"
  }

  protocols {
    enable_http2 = true
  }

  security {
    enable_backend_ssl30  = false
    enable_backend_tls10  = false
    enable_backend_tls11  = false
    enable_frontend_ssl30 = false
    enable_frontend_tls10 = false
    enable_frontend_tls11 = false
  }
}

# Define API
resource "azurerm_api_management_api" "backend_api" {
  name                = "backend-api"
  resource_group_name = var.resource_group_name
  api_management_name = azurerm_api_management.apim.name
  revision            = "1"
  display_name        = "Backend API"
  path                = "api"
  protocols           = ["https"]
  service_url         = "https://backend.${var.environment}.internal"

  subscription_required = true

  oauth2_authorization {
    authorization_server_name = "azure-ad"
  }
}

# Add policy (rate limiting)
resource "azurerm_api_management_api_policy" "rate_limit" {
  api_name            = azurerm_api_management_api.backend_api.name
  api_management_name = azurerm_api_management.apim.name
  resource_group_name = var.resource_group_name

  xml_content = <<XML
<policies>
  <inbound>
    <rate-limit-by-key calls="100" renewal-period="60" counter-key="@(context.Request.IpAddress)" />
    <quota-by-key calls="10000" renewal-period="86400" counter-key="@(context.Subscription.Id)" />
    <validate-jwt header-name="Authorization" failed-validation-httpcode="401">
      <openid-config url="https://login.microsoftonline.com/${var.tenant_id}/.well-known/openid-configuration" />
      <audiences>
        <audience>api://${var.backend_client_id}</audience>
      </audiences>
    </validate-jwt>
  </inbound>
  <backend>
    <forward-request />
  </backend>
  <outbound />
  <on-error />
</policies>
XML
}
```

**Cost Considerations**:
- Developer: ~$50/month (for dev/test)
- Basic: ~$150/month
- Standard: ~$700/month
- Premium: ~$2,800/month (multi-region, higher throughput)

**Why This Service**:
- Centralized API management
- Built-in security policies
- Developer portal for API documentation
- Analytics and monitoring
- Decouples frontend from backend changes

---

### 6. Azure Entra ID (Azure Active Directory)

**Purpose**: Identity and access management

**Key Features Used**:
- Application registrations (OAuth2 apps)
- User authentication
- Token issuance (JWT)
- API permissions (scopes)
- Admin consent for applications
- Conditional access (optional)
- MFA (optional)
- B2C integration (optional for customer identities)

**Authentication Flow**:

```
┌──────────┐                                      ┌──────────────┐
│          │  1. User clicks "Login"              │              │
│  User    │─────────────────────────────────────▶│   Frontend   │
│          │                                      │   (React)    │
└──────────┘                                      └──────┬───────┘
                                                         │
                    2. Redirect to Azure AD login       │
                       with client_id, scope, etc.      │
                                                         │
                                                         ▼
                                              ┌─────────────────────┐
                                              │                     │
                                              │   Azure Entra ID    │
                                              │   (Azure AD)        │
                                              │                     │
                                              └──────────┬──────────┘
                                                         │
                    3. User enters credentials          │
                       and grants consent               │
                                                         │
                                                         ▼
┌──────────┐                                      ┌─────────────────┐
│          │  4. Redirect with authorization code │                 │
│  User    │◀─────────────────────────────────────│   Azure AD      │
│          │                                      │                 │
└──────────┘                                      └─────────────────┘
     │
     │  5. Frontend exchanges code for token
     │     (MSAL.js handles this automatically)
     │
     ▼
┌──────────────┐                                  ┌─────────────────┐
│              │  6. Get access token             │                 │
│   Frontend   │◀─────────────────────────────────│   Azure AD      │
│   (React)    │                                  │                 │
└──────┬───────┘                                  └─────────────────┘
       │
       │  7. Call API with Bearer token
       │     Authorization: Bearer eyJ0eXAiOiJKV1QiLCJh...
       │
       ▼
┌──────────────┐
│              │  8. Validate token (JWT signature, exp, aud, iss)
│   Backend    │     using Azure AD public keys
│  (Spring)    │
│              │  9. Extract user info (claims) from token
└──────────────┘
       │
       │  10. Return protected resource
       │
       ▼
┌──────────────┐
│   Frontend   │  11. Display data to user
│              │
└──────────────┘
```

**App Registration Configuration**:

**Frontend (SPA)**:
- Application type: Single-page application
- Redirect URIs: https://yourdomain.com, http://localhost:3000
- Token configuration: ID tokens, Access tokens
- API permissions: Delegated permissions to Backend API

**Backend (API)**:
- Application type: Web API
- Expose an API: api://{client-id}/access_as_user
- App roles: Admin, User (optional)
- Token configuration: Access tokens

**Cost Considerations**:
- Free tier: 50,000 monthly active users
- Premium P1: ~$6/user/month (includes MFA, conditional access)
- Premium P2: ~$9/user/month (includes advanced protection)

**Why This Service**:
- Enterprise SSO
- Industry-standard OAuth2/OpenID Connect
- No custom authentication code needed
- Audit trail and compliance
- Integration with Microsoft 365 (optional)

---

### 7. Log Analytics Workspace

**Purpose**: Centralized log aggregation and analysis

**Key Features Used**:
- Centralized log storage
- KQL (Kusto Query Language) for log analysis
- Integration with all Azure resources
- Custom log ingestion
- Log retention policies (30, 60, 90, 365 days)
- Workbooks for dashboards
- Alert rules based on log queries

**Configuration**:
```hcl
resource "azurerm_log_analytics_workspace" "law" {
  name                = "law-${var.project_name}-${var.environment}"
  location            = var.location
  resource_group_name = var.resource_group_name
  sku                 = "PerGB2018"
  retention_in_days   = 30  # 30, 60, 90, 120, 180, 365, 730

  daily_quota_gb = 1  # Prevent runaway costs
}

# Connect AKS
resource "azurerm_log_analytics_solution" "container_insights" {
  solution_name         = "ContainerInsights"
  location              = var.location
  resource_group_name   = var.resource_group_name
  workspace_resource_id = azurerm_log_analytics_workspace.law.id
  workspace_name        = azurerm_log_analytics_workspace.law.name

  plan {
    publisher = "Microsoft"
    product   = "OMSGallery/ContainerInsights"
  }
}
```

**Sample Queries**:

**View all container logs**:
```kql
ContainerLog
| where TimeGenerated > ago(1h)
| where Name contains "backend"
| project TimeGenerated, Name, LogEntry
| order by TimeGenerated desc
```

**Count errors in last hour**:
```kql
ContainerLog
| where TimeGenerated > ago(1h)
| where LogEntry contains "ERROR"
| summarize count() by Name
```

**Track API response times**:
```kql
AppRequests
| where TimeGenerated > ago(24h)
| summarize avg(DurationMs), p50=percentile(DurationMs, 50), p95=percentile(DurationMs, 95) by Name
| order by avg_DurationMs desc
```

**Cost Considerations**:
- Pay-per-GB ingestion: ~$2.30/GB
- Data retention (first 31 days free, then $0.10/GB/month)
- Typical usage: 1-5 GB/day = ~$70-350/month

**Why This Service**:
- Centralized logging across all services
- Powerful query language (KQL)
- Integration with alerting
- Compliance and audit requirements

---

### 8. Application Insights

**Purpose**: Application Performance Monitoring (APM)

**Key Features Used**:
- Automatic instrumentation for Spring Boot
- Request tracking (HTTP requests)
- Dependency tracking (database, external APIs)
- Exception tracking
- Custom events and metrics
- Live metrics stream
- Application map (visualize dependencies)
- Availability tests (ping tests)
- Smart detection (anomaly detection)

**Configuration**:

**Backend (Spring Boot)**:
```yaml
# application.yml
spring:
  application:
    name: aks-starter-backend

management:
  endpoints:
    web:
      exposure:
        include: "*"

# Add Application Insights SDK
# pom.xml
<dependency>
  <groupId>com.microsoft.azure</groupId>
  <artifactId>applicationinsights-spring-boot-starter</artifactId>
  <version>2.6.4</version>
</dependency>
```

**Environment Variable**:
```bash
APPLICATIONINSIGHTS_CONNECTION_STRING=InstrumentationKey=xxxxx;...
```

**Custom Telemetry**:
```java
@Autowired
private TelemetryClient telemetryClient;

public void processOrder(Order order) {
    // Track custom event
    telemetryClient.trackEvent("OrderProcessed",
        Map.of("orderId", order.getId(), "amount", order.getAmount()),
        Map.of("itemCount", order.getItems().size()));

    // Track custom metric
    telemetryClient.trackMetric("OrderValue", order.getAmount());

    try {
        // Business logic
    } catch (Exception e) {
        // Track exception
        telemetryClient.trackException(e);
    }
}
```

**Cost Considerations**:
- First 5 GB/month: Free
- Additional data: ~$2.30/GB
- Data retention (first 90 days free, then $0.10/GB/month)
- Typical usage: 2-10 GB/month = Free to ~$12/month

**Why This Service**:
- Deep application insights
- Automatic dependency mapping
- Anomaly detection
- Seamless Azure integration

---

### 9. Azure Monitor

**Purpose**: Unified monitoring and alerting platform

**Key Features Used**:
- Metrics collection (CPU, memory, network, disk)
- Alert rules with action groups
- Metric-based autoscaling
- Workbooks (custom dashboards)
- Activity logs (Azure resource changes)
- Service health monitoring

**Alert Examples**:

**High CPU Alert**:
```hcl
resource "azurerm_monitor_metric_alert" "high_cpu" {
  name                = "high-cpu-alert"
  resource_group_name = var.resource_group_name
  scopes              = [azurerm_kubernetes_cluster.aks.id]
  description         = "Alert when CPU usage is above 80%"

  criteria {
    metric_namespace = "Microsoft.ContainerService/managedClusters"
    metric_name      = "node_cpu_usage_percentage"
    aggregation      = "Average"
    operator         = "GreaterThan"
    threshold        = 80
  }

  action {
    action_group_id = azurerm_monitor_action_group.main.id
  }
}
```

**Action Group (Email/SMS)**:
```hcl
resource "azurerm_monitor_action_group" "main" {
  name                = "critical-alerts"
  resource_group_name = var.resource_group_name
  short_name          = "critical"

  email_receiver {
    name          = "sendtoadmin"
    email_address = "admin@example.com"
  }

  sms_receiver {
    name         = "sendtoonCall"
    country_code = "1"
    phone_number = "1234567890"
  }

  webhook_receiver {
    name        = "sendtoteams"
    service_uri = "https://outlook.office.com/webhook/..."
  }
}
```

**Cost Considerations**:
- Metrics: First 10 metrics free, then $0.10/metric/month
- Alerts: ~$0.10/alert evaluation
- Typically <$20/month for this solution

**Why This Service**:
- Unified monitoring across Azure
- Proactive alerting
- Integration with autoscaling
- Custom dashboards

---

## Network Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              INTERNET                                    │
└──────────────────────────────┬──────────────────────────────────────────┘
                               │
                               │  HTTPS (443)
                               │
                               ▼
                    ┌──────────────────────┐
                    │  Public IP Address   │
                    │  X.X.X.X             │
                    │                      │
                    │  DNS A Record:       │
                    │  yourdomain.com      │
                    └──────────┬───────────┘
                               │
┌──────────────────────────────┼──────────────────────────────────────────┐
│  VIRTUAL NETWORK (10.0.0.0/16)             │                             │
│                               │                                          │
│  ┌────────────────────────────┼────────────────────────────────────┐    │
│  │  SUBNET: AppGW Subnet      ▼                                    │    │
│  │  Address Range: 10.0.1.0/24                                     │    │
│  │                                                                  │    │
│  │  ┌───────────────────────────────────────────────────┐          │    │
│  │  │  Application Gateway (WAF v2)                     │          │    │
│  │  │                                                    │          │    │
│  │  │  • SSL/TLS termination                            │          │    │
│  │  │  • WAF rules (OWASP 3.2)                          │          │    │
│  │  │  • Backend pool: 10.0.2.10 (AKS ingress)          │          │    │
│  │  │  • Health probe: /health                          │          │    │
│  │  └───────────────────────────────────────────────────┘          │    │
│  └──────────────────────────────────────────────────────────────────┘    │
│                               │                                          │
│                               │  HTTP (80) - Internal only               │
│                               │                                          │
│  ┌────────────────────────────┼────────────────────────────────────┐    │
│  │  SUBNET: AKS Subnet        ▼                                    │    │
│  │  Address Range: 10.0.2.0/23                                     │    │
│  │                                                                  │    │
│  │  ┌───────────────────────────────────────────────────┐          │    │
│  │  │  NGINX Ingress Controller (LoadBalancer)          │          │    │
│  │  │  Internal IP: 10.0.2.10                           │          │    │
│  │  │                                                    │          │    │
│  │  │  Rules:                                            │          │    │
│  │  │    • / → frontend-svc:80                          │          │    │
│  │  │    • /api/* → backend-svc:8080                    │          │    │
│  │  └────────────────┬──────────────────────────────────┘          │    │
│  │                   │                                              │    │
│  │  ┌────────────────┼──────────────────────────────────┐          │    │
│  │  │  AKS Cluster   │                                  │          │    │
│  │  │                │                                  │          │    │
│  │  │  ┌─────────────▼──────────┐  ┌─────────────────┐ │          │    │
│  │  │  │  Frontend Service      │  │ Backend Service │ │          │    │
│  │  │  │  ClusterIP: 10.0.2.100 │  │ ClusterIP:      │ │          │    │
│  │  │  │  Port: 80              │  │ 10.0.2.101:8080 │ │          │    │
│  │  │  └────────────┬───────────┘  └────────┬────────┘ │          │    │
│  │  │               │                       │          │          │    │
│  │  │  ┌────────────▼──────┐   ┌───────────▼─────┐    │          │    │
│  │  │  │  Frontend Pods    │   │  Backend Pods   │    │          │    │
│  │  │  │                   │   │                 │    │          │    │
│  │  │  │  • Pod IPs:       │   │  • Pod IPs:     │    │          │    │
│  │  │  │    10.0.2.50      │   │    10.0.2.60    │    │          │    │
│  │  │  │    10.0.2.51      │   │    10.0.2.61    │    │          │    │
│  │  │  │    10.0.2.52      │   │    10.0.2.62    │    │          │    │
│  │  │  └───────────────────┘   └─────────────────┘    │          │    │
│  │  │                                                  │          │    │
│  │  └──────────────────────────────────────────────────┘          │    │
│  │                                                                  │    │
│  │  Network Policies (Calico):                                     │    │
│  │    • Deny all by default                                        │    │
│  │    • Allow ingress to frontend from ingress controller          │    │
│  │    • Allow ingress to backend from frontend                     │    │
│  │    • Allow egress to Azure services (Key Vault, ACR, etc.)      │    │
│  └──────────────────────────────────────────────────────────────────┘    │
│                                                                           │
│  ┌───────────────────────────────────────────────────────────────┐      │
│  │  SUBNET: Private Endpoint Subnet (Optional)                   │      │
│  │  Address Range: 10.0.4.0/24                                   │      │
│  │                                                                │      │
│  │  Private Endpoints:                                            │      │
│  │    • Key Vault: kv-aksstarter.privatelink.vaultcore.azure.net │      │
│  │    • ACR: acr.privatelink.azurecr.io                          │      │
│  │    • Storage: st.privatelink.blob.core.windows.net            │      │
│  └───────────────────────────────────────────────────────────────┘      │
│                                                                           │
└───────────────────────────────────────────────────────────────────────────┘

NSG Rules:
┌────────────────────────────────────────────────────────────────┐
│  AppGW Subnet NSG:                                             │
│    • Allow inbound 443 from Internet                           │
│    • Allow inbound 65200-65535 (Azure infrastructure)          │
│    • Allow outbound 80 to AKS subnet                           │
│    • Deny all other inbound                                    │
│                                                                │
│  AKS Subnet NSG:                                               │
│    • Allow inbound 80 from AppGW subnet                        │
│    • Allow inbound 443 for Kubernetes API (from authorized)    │
│    • Allow all outbound (for Azure services, internet)         │
│    • Calico network policies provide pod-level control         │
└────────────────────────────────────────────────────────────────┘
```

**Key Network Features**:
- **Azure CNI**: Pods get IPs from VNet (no NAT needed)
- **Calico Network Policies**: Micro-segmentation between pods
- **Private Cluster** (optional): API server not exposed to internet
- **Private Endpoints** (optional): PaaS services accessed via VNet
- **Service Endpoints**: Alternative to private endpoints (lower cost)

---

## Security Architecture

### Defense in Depth

```
┌─────────────────────────────────────────────────────────────────┐
│  LAYER 1: PERIMETER SECURITY                                    │
│                                                                 │
│  ✓ Azure DDoS Protection (Standard - optional)                 │
│  ✓ Azure Front Door (optional - global WAF)                    │
│  ✓ Application Gateway WAF v2 (OWASP rules)                    │
│  ✓ IP whitelisting (if required)                               │
│  ✓ Geo-filtering (if required)                                 │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  LAYER 2: NETWORK SECURITY                                      │
│                                                                 │
│  ✓ Virtual Network (VNet) isolation                            │
│  ✓ Network Security Groups (NSGs)                              │
│  ✓ Subnet segmentation                                         │
│  ✓ Calico network policies (pod-to-pod control)                │
│  ✓ Private endpoints for PaaS services                         │
│  ✓ No public IPs on worker nodes                               │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  LAYER 3: IDENTITY & ACCESS                                     │
│                                                                 │
│  ✓ Azure AD authentication                                     │
│  ✓ OAuth2/OpenID Connect                                       │
│  ✓ Managed identities (no credentials in code)                 │
│  ✓ RBAC (Azure and Kubernetes)                                 │
│  ✓ Service principals with least privilege                     │
│  ✓ MFA for admin access                                        │
│  ✓ Conditional access policies                                 │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  LAYER 4: APPLICATION SECURITY                                  │
│                                                                 │
│  ✓ API Management (rate limiting, throttling)                  │
│  ✓ Input validation (Bean Validation)                          │
│  ✓ Output encoding                                             │
│  ✓ CSRF protection                                             │
│  ✓ CORS configuration                                          │
│  ✓ Secure headers (HSTS, CSP, X-Frame-Options)                 │
│  ✓ No secrets in code (use Key Vault)                          │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  LAYER 5: CONTAINER SECURITY                                    │
│                                                                 │
│  ✓ Container image scanning (Trivy in CI/CD)                   │
│  ✓ Use minimal base images (distroless, alpine)                │
│  ✓ Non-root user in containers                                 │
│  ✓ Read-only root filesystem                                   │
│  ✓ Resource limits (CPU, memory)                               │
│  ✓ Pod security policies/standards                             │
│  ✓ Signed images (Content Trust)                               │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  LAYER 6: DATA SECURITY                                         │
│                                                                 │
│  ✓ Encryption in transit (TLS 1.2+)                            │
│  ✓ Encryption at rest (Azure Storage, AKS disks)               │
│  ✓ Key Vault for secrets/certificates                          │
│  ✓ Database encryption (if using Azure DB)                     │
│  ✓ Backup encryption                                            │
│  ✓ Data classification and DLP                                 │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  LAYER 7: MONITORING & COMPLIANCE                               │
│                                                                 │
│  ✓ Azure Security Center (Microsoft Defender for Cloud)        │
│  ✓ Azure Sentinel (SIEM - optional)                            │
│  ✓ Audit logging (all Azure resources)                         │
│  ✓ Container insights                                           │
│  ✓ Compliance dashboards                                       │
│  ✓ Vulnerability scanning                                      │
│  ✓ Security alerts and incident response                       │
└─────────────────────────────────────────────────────────────────┘
```

### Security Controls Matrix

| Control | Implementation | Azure Service |
|---------|----------------|---------------|
| **Authentication** | OAuth2/OpenID Connect | Azure AD |
| **Authorization** | RBAC, Azure AD roles | Azure AD, AKS RBAC |
| **Secrets Management** | Centralized vault | Azure Key Vault |
| **Network Isolation** | VNet, subnets, NSGs | VNet, NSG |
| **Micro-segmentation** | Network policies | Calico |
| **Web Protection** | WAF with OWASP rules | Application Gateway WAF |
| **API Protection** | Rate limiting, OAuth2 | API Management |
| **DDoS Protection** | L3/L4 protection | Azure DDoS (optional) |
| **Container Scanning** | CI/CD integration | Trivy |
| **Code Quality** | Static analysis | SonarQube |
| **IaC Security** | Terraform scanning | tfsec, tflint |
| **Dynamic Testing** | OWASP ZAP | Pipeline integration |
| **Encryption (Transit)** | TLS 1.2+ | App Gateway, APIM |
| **Encryption (Rest)** | AES-256 | Azure Storage, AKS |
| **Audit Logging** | All operations logged | Log Analytics |
| **Monitoring** | 24/7 monitoring | Azure Monitor |
| **Incident Response** | Alerts and action groups | Azure Monitor, Action Groups |
| **Compliance** | PCI-DSS, HIPAA, SOC 2 | Azure Policy, Security Center |

---

## Data Flow

### User Request Flow (End-to-End)

```
┌──────┐
│ User │
└───┬──┘
    │
    │ 1. Browse to https://yourdomain.com
    │
    ▼
┌────────────────────┐
│  DNS Resolution    │  A record: yourdomain.com → X.X.X.X (App Gateway IP)
└─────────┬──────────┘
          │
          │ 2. HTTPS request to X.X.X.X
          │
          ▼
┌─────────────────────────────────────────────┐
│  Application Gateway                        │
│                                             │
│  • Terminate SSL/TLS                        │
│  • WAF inspection (OWASP rules)             │
│  • Route based on path:                     │
│    - / → AKS frontend                       │
│    - /api/* → AKS backend                   │
│  • Add headers (X-Forwarded-For, etc.)      │
└──────────────┬──────────────────────────────┘
               │
               │ 3. HTTP request to AKS ingress (10.0.2.10)
               │
               ▼
┌─────────────────────────────────────────────┐
│  AKS Cluster - NGINX Ingress                │
│                                             │
│  • TLS termination (if using cert-manager)  │
│  • Route to appropriate service:            │
│    - / → frontend-svc                       │
│    - /api/* → backend-svc                   │
└──────────────┬──────────────────────────────┘
               │
               ├────────────────┬─────────────────────┐
               │                │                     │
               ▼                ▼                     ▼
       ┌─────────────┐  ┌─────────────┐     ┌─────────────┐
       │ Frontend    │  │ Frontend    │     │ Frontend    │
       │ Pod 1       │  │ Pod 2       │     │ Pod 3       │
       │ 10.0.2.50   │  │ 10.0.2.51   │     │ 10.0.2.52   │
       └─────────────┘  └─────────────┘     └─────────────┘
               │
               │ 4. Serve index.html (React SPA)
               │
               ▼
┌──────────────────────────┐
│  User's Browser          │
│                          │
│  • Load React app        │
│  • Initialize MSAL       │
│  • Check if user logged in │
└────────┬─────────────────┘
         │
         │ 5. If not logged in, redirect to Azure AD
         │
         ▼
┌────────────────────────────────┐
│  Azure AD Login Page           │
│                                │
│  • User enters credentials     │
│  • MFA (if enabled)            │
│  • Grant consent               │
└────────┬───────────────────────┘
         │
         │ 6. Redirect with auth code
         │
         ▼
┌──────────────────────────┐
│  React App (MSAL.js)     │
│                          │
│  • Exchange code for token │
│  • Store token in session  │
│  • Navigate to home page   │
└────────┬─────────────────┘
         │
         │ 7. User clicks "Call API" button
         │
         ▼
┌──────────────────────────┐
│  Frontend JavaScript     │
│                          │
│  • Acquire token silently  │
│  • Make API request:       │
│    GET /api/hello          │
│    Authorization: Bearer eyJ... │
└────────┬─────────────────┘
         │
         │ 8. HTTPS request to backend
         │
         ▼
┌─────────────────────────────────────────────┐
│  Application Gateway                        │
│                                             │
│  • WAF inspection                           │
│  • Route /api/* → backend                   │
└──────────────┬──────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────┐
│  API Management (Optional)                  │
│                                             │
│  • Rate limiting check                      │
│  • Validate JWT token                       │
│  • Check quota                              │
│  • Log request                              │
└──────────────┬──────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────┐
│  AKS - NGINX Ingress                        │
│                                             │
│  • Route to backend-svc                     │
└──────────────┬──────────────────────────────┘
               │
               ├────────────────┬─────────────────────┐
               │                │                     │
               ▼                ▼                     ▼
       ┌─────────────┐  ┌─────────────┐     ┌─────────────┐
       │ Backend     │  │ Backend     │     │ Backend     │
       │ Pod 1       │  │ Pod 2       │     │ Pod 3       │
       │ 10.0.2.60   │  │ 10.0.2.61   │     │ 10.0.2.62   │
       └─────────────┘  └─────────────┘     └─────────────┘
               │
               │ 9. Spring Security validates JWT:
               │    - Check signature (Azure AD public key)
               │    - Check expiry (exp claim)
               │    - Check audience (aud claim)
               │    - Check issuer (iss claim)
               │
               ▼
┌─────────────────────────────────────────────┐
│  Spring Boot Controller                     │
│                                             │
│  @GetMapping("/hello")                      │
│  public ResponseEntity<HelloResponse>       │
│      hello(Authentication auth) {           │
│                                             │
│    String username = auth.getName();        │
│    HelloResponse response =                 │
│        helloService.getHelloMessage(username); │
│                                             │
│    return ResponseEntity.ok(response);      │
│  }                                          │
└──────────────┬──────────────────────────────┘
               │
               │ 10. Return JSON response
               │
               ▼
┌──────────────────────────┐
│  User's Browser          │
│                          │
│  • Display API response  │
│  • Show "Hello, John!"   │
└──────────────────────────┘

┌─────────────────────────────────────────────┐
│  TELEMETRY (throughout the flow)            │
│                                             │
│  • App Gateway → Log Analytics              │
│  • APIM → Log Analytics                     │
│  • AKS pods → Container Insights            │
│  • Spring Boot → Application Insights       │
│  • React → Application Insights (custom)    │
│  • All metrics → Azure Monitor              │
└─────────────────────────────────────────────┘
```

---

## Deployment Architecture

### CI/CD Pipeline Flow

```
Developer Workflow:
┌──────────────────────────────────────────────────────────────┐
│  Local Development                                           │
│                                                              │
│  1. Code changes                                             │
│  2. Run tests locally                                        │
│  3. Create feature branch                                    │
│  4. Commit and push                                          │
│  5. Create pull request                                      │
└──────────────┬───────────────────────────────────────────────┘
               │
               ▼
┌──────────────────────────────────────────────────────────────┐
│  Pull Request Validation                                     │
│                                                              │
│  ✓ Checkstyle validation                                    │
│  ✓ Spotless format check                                    │
│  ✓ Unit tests (JUnit/Jest)                                  │
│  ✓ Code coverage threshold check (70%)                      │
│  ✓ SonarQube quality gate                                   │
│  ✓ Peer review                                               │
└──────────────┬───────────────────────────────────────────────┘
               │
               │ PR Approved & Merged to main
               │
               ▼
┌──────────────────────────────────────────────────────────────┐
│  CI Pipeline (Build & Test)                                 │
│                                                              │
│  Frontend Pipeline:                Backend Pipeline:         │
│  ┌─────────────────┐            ┌─────────────────┐         │
│  │ npm install     │            │ mvn clean       │         │
│  │ npm test        │            │ mvn test        │         │
│  │ npm run build   │            │ mvn verify      │         │
│  │ docker build    │            │ docker build    │         │
│  │ trivy scan      │            │ trivy scan      │         │
│  │ docker push ACR │            │ docker push ACR │         │
│  └─────────────────┘            └─────────────────┘         │
│                                                              │
│  Artifacts Created:                                          │
│    • Docker images (tagged with build ID)                   │
│    • Test reports                                            │
│    • Code coverage reports                                   │
│    • Security scan results                                   │
│    • Cucumber HTML reports                                   │
└──────────────┬───────────────────────────────────────────────┘
               │
               ▼
┌──────────────────────────────────────────────────────────────┐
│  CD Pipeline (Deploy)                                        │
│                                                              │
│  ┌────────────────────────────────────────────────┐         │
│  │  DEV Environment                                │         │
│  │                                                 │         │
│  │  • Get AKS credentials                          │         │
│  │  • helm upgrade backend --namespace dev         │         │
│  │  • helm upgrade frontend --namespace dev        │         │
│  │  • kubectl rollout status                       │         │
│  │  • Run smoke tests                              │         │
│  │  • Publish results                              │         │
│  │                                                 │         │
│  │  Status: ✓ Deployed                            │         │
│  └────────────────────────────────────────────────┘         │
│                         │                                    │
│                         │ Manual Approval                    │
│                         │                                    │
│  ┌────────────────────────────────────────────────┐         │
│  │  STAGE Environment                              │         │
│  │                                                 │         │
│  │  • Deploy to stage AKS cluster                  │         │
│  │  • Run integration tests                        │         │
│  │  • Run OWASP ZAP scan                           │         │
│  │  • Performance testing (optional)               │         │
│  │  • Manual exploratory testing                   │         │
│  │                                                 │         │
│  │  Status: ✓ Deployed & Tested                   │         │
│  └────────────────────────────────────────────────┘         │
│                         │                                    │
│                         │ Manual Approval (2 approvers)      │
│                         │                                    │
│  ┌────────────────────────────────────────────────┐         │
│  │  PROD Environment                               │         │
│  │                                                 │         │
│  │  • Deploy to prod AKS cluster                   │         │
│  │  • Blue-Green deployment (optional)             │         │
│  │  • Health check validation                      │         │
│  │  • Monitor for 5 minutes                        │         │
│  │  • Send deployment notification                 │         │
│  │                                                 │         │
│  │  Status: ✓ Live in Production                  │         │
│  └────────────────────────────────────────────────┘         │
└──────────────────────────────────────────────────────────────┘
```

---

## Monitoring & Observability

### Three Pillars of Observability

**1. Metrics** (What is happening)
- Azure Monitor metrics
- Prometheus metrics from pods
- Custom application metrics (Application Insights)
- Infrastructure metrics (CPU, memory, network, disk)

**2. Logs** (Detailed event data)
- Container logs (stdout/stderr)
- Application logs (structured JSON)
- Azure resource logs (Activity logs, Diagnostic logs)
- Audit logs (Key Vault access, RBAC changes)

**3. Traces** (Distributed tracing)
- Application Insights distributed tracing
- Request correlation across services
- Dependency tracking
- Performance bottleneck identification

### Monitoring Dashboard Example

```
┌─────────────────────────────────────────────────────────────────────┐
│  AZURE MONITOR WORKBOOK - PRODUCTION DASHBOARD                      │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  📊 OVERALL HEALTH                          ⏰ Last 24 hours       │
│  ┌────────────────┬────────────────┬────────────────┬────────────┐ │
│  │ Availability   │  Request Rate  │  Error Rate    │  Avg Resp  │ │
│  │   99.95%  ✓   │  1,234 req/min │   0.05%   ✓   │  145ms  ✓  │ │
│  └────────────────┴────────────────┴────────────────┴────────────┘ │
│                                                                     │
│  📈 AKS CLUSTER METRICS                                             │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Node CPU Usage (%)                                         │   │
│  │  ████████████░░░░░░░░ 60%                                   │   │
│  │                                                             │   │
│  │  Node Memory Usage (%)                                      │   │
│  │  ██████████████░░░░░░ 70%                                   │   │
│  │                                                             │   │
│  │  Active Pods: 24/50                                         │   │
│  │  Active Nodes: 5/15 (autoscaling enabled)                  │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  🎯 APPLICATION METRICS                                             │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Backend API Requests (last hour)                           │   │
│  │  [Chart: Line graph showing request rate]                   │   │
│  │                                                             │   │
│  │  Top 5 Endpoints by Traffic:                                │   │
│  │    1. GET /api/hello         - 45% (556 req/min)           │   │
│  │    2. GET /api/users         - 25% (309 req/min)           │   │
│  │    3. POST /api/users        - 15% (185 req/min)           │   │
│  │    4. GET /api/health        - 10% (124 req/min)           │   │
│  │    5. GET /actuator/metrics  - 5%  (62 req/min)            │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  ⚠️  ACTIVE ALERTS (2)                                             │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  [WARNING] High memory usage on node aks-pool-1-abc123     │   │
│  │            Memory: 85% (threshold: 80%)                     │   │
│  │            Time: 10:45 AM                                    │   │
│  │                                                             │   │
│  │  [INFO] Slow response time for /api/users endpoint         │   │
│  │         P95 latency: 850ms (threshold: 500ms)               │   │
│  │         Time: 10:30 AM                                       │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  📝 RECENT ERRORS (Last 1 hour)                                    │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  [ERROR] NullPointerException in UserService.getUserById   │   │
│  │          Count: 3                                            │   │
│  │          First seen: 10:52 AM                                │   │
│  │          [View in Application Insights]                     │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Disaster Recovery

### Backup Strategy

**AKS Cluster**:
- Cluster configuration stored in Git (GitOps)
- Velero for cluster backup (optional)
- Helm charts version controlled

**Container Images**:
- Stored in ACR with geo-replication
- Retention policy: Keep all tagged images
- Automated cleanup of untagged images (>30 days)

**Secrets**:
- Key Vault with soft delete enabled
- Key Vault backup (automated)
- Secret rotation policy

**Configuration**:
- Infrastructure: Terraform state in Azure Storage (versioned)
- Application: Helm values in Git
- Pipelines: Azure DevOps (built-in backup)

### Recovery Time Objective (RTO) & Recovery Point Objective (RPO)

| Component | RPO | RTO | Recovery Method |
|-----------|-----|-----|-----------------|
| **AKS Cluster** | 0 (IaC) | 30 min | Terraform apply |
| **Application** | 0 (Git) | 10 min | Helm install/upgrade |
| **Container Images** | 0 (ACR) | 5 min | Pull from ACR |
| **Secrets** | 0 (KV) | 5 min | Key Vault restore |
| **Configuration** | 0 (Git) | 5 min | Git checkout |

### Disaster Recovery Scenarios

**Scenario 1: Single Pod Failure**
- Detection: Kubernetes liveness probe failure
- Action: Automatic pod restart by Kubernetes
- RTO: <1 minute

**Scenario 2: Node Failure**
- Detection: Node becomes NotReady
- Action: Kubernetes reschedules pods to healthy nodes
- RTO: 1-3 minutes

**Scenario 3: Cluster Failure**
- Detection: Unable to reach Kubernetes API
- Action: Deploy new cluster from Terraform, restore applications from Git
- RTO: 30-45 minutes

**Scenario 4: Region Failure**
- Detection: Azure region outage
- Action: Failover to secondary region (if multi-region setup)
- RTO: 1-2 hours (manual failover), or use Azure Traffic Manager for automatic

**Scenario 5: Data Corruption**
- Detection: Application logic error, corrupted secrets
- Action: Restore from Key Vault backup or Git history
- RTO: 15-30 minutes

---

## Summary of Azure Services and Costs

| Azure Service | Purpose | Est. Monthly Cost (Prod) |
|---------------|---------|--------------------------|
| **AKS** | Container orchestration | $210-1,050 (3-15 nodes) |
| **ACR** | Container registry | $50 (Premium) |
| **Key Vault** | Secrets management | $5 |
| **App Gateway WAF v2** | Layer 7 LB + WAF | $300-400 |
| **API Management** | API gateway | $50-700 (tier-dependent) |
| **Log Analytics** | Centralized logging | $70-350 (1-5 GB/day) |
| **Application Insights** | APM | Free-$12 (< 10 GB) |
| **Azure Monitor** | Metrics & alerting | $20 |
| **Storage Account** | Terraform state | $2 |
| **Public IP** | App Gateway | $4 |
| **VNet** | Networking | Free |
| **NSG** | Network security | Free |
| **Azure AD** | Authentication | Free (or $6/user P1) |
| **TOTAL (Dev)** | | ~$300-400/month |
| **TOTAL (Prod)** | | ~$700-2,600/month |

**Cost Optimization Tips**:
- Use Reserved Instances for VMs (30-60% savings)
- Enable autoscaling (scale down during off-hours)
- Use Dev/Test pricing for non-production
- Monitor and rightsize resources
- Use Azure Cost Management for tracking

---

## Conclusion

This architecture provides:

✅ **Enterprise-grade security** - Multiple layers of defense
✅ **High availability** - Multi-zone AKS, HPA, PDB, health probes
✅ **Scalability** - Autoscaling at pod and node levels
✅ **Observability** - Comprehensive logging, metrics, and tracing
✅ **Compliance** - Audit logs, encryption, RBAC
✅ **DevOps automation** - Full CI/CD with quality gates
✅ **Cost optimization** - Autoscaling, proper sizing
✅ **Disaster recovery** - IaC, backups, documented procedures

The solution is production-ready and can be customized for specific business requirements.
