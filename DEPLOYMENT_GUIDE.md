# Azure DevOps Deployment Guide

## Complete Guide: From Code Check-in to Production Deployment

This guide walks you through setting up Azure DevOps pipelines to build and deploy the AKS Starter Kit to Azure.

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Azure Setup](#azure-setup)
3. [Azure DevOps Setup](#azure-devops-setup)
4. [Pipeline Configuration](#pipeline-configuration)
5. [First Deployment](#first-deployment)
6. [Deployment Flow](#deployment-flow)
7. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Tools

- [ ] Azure CLI (`az`) - [Install](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli)
- [ ] Azure subscription with Owner or Contributor access
- [ ] Azure DevOps organization - [Create](https://dev.azure.com)
- [ ] Git installed locally
- [ ] kubectl installed - [Install](https://kubernetes.io/docs/tasks/tools/)
- [ ] Terraform CLI (optional, for local testing) - [Install](https://www.terraform.io/downloads)

### Required Knowledge

- Basic understanding of Azure services
- Git version control
- YAML syntax
- Kubernetes basics
- Azure DevOps pipelines

---

## Azure Setup

### Step 1: Azure AD (Entra ID) Application Registration

#### 1.1 Register Backend API Application

```bash
# Login to Azure
az login

# Set your subscription
az account set --subscription "YOUR_SUBSCRIPTION_ID"

# Register the backend API app
az ad app create \
  --display-name "aksstarter-backend-api" \
  --sign-in-audience "AzureADMyOrg" \
  --enable-access-token-issuance true \
  --enable-id-token-issuance false

# Note the Application (client) ID from the output
export BACKEND_CLIENT_ID="<output-app-id>"

# Create a service principal for the app
az ad sp create --id $BACKEND_CLIENT_ID

# Expose an API scope
az ad app update --id $BACKEND_CLIENT_ID \
  --identifier-uris "api://$BACKEND_CLIENT_ID"

# Add API scope (access_as_user)
cat > manifest.json <<EOF
{
  "oauth2PermissionScopes": [
    {
      "adminConsentDescription": "Allow the application to access the API on behalf of the signed-in user",
      "adminConsentDisplayName": "Access API",
      "id": "$(uuidgen)",
      "isEnabled": true,
      "type": "User",
      "userConsentDescription": "Allow the application to access the API on your behalf",
      "userConsentDisplayName": "Access API",
      "value": "access_as_user"
    }
  ]
}
EOF

az ad app update --id $BACKEND_CLIENT_ID --set api=@manifest.json
```

**Record these values:**
```
Backend Client ID: ___________________________
Tenant ID: ___________________________________
API Scope: api://<BACKEND_CLIENT_ID>/access_as_user
```

#### 1.2 Register Frontend SPA Application

```bash
# Register the frontend SPA app
az ad app create \
  --display-name "aksstarter-frontend-spa" \
  --sign-in-audience "AzureADMyOrg" \
  --web-redirect-uris "http://localhost:3000" "https://yourdomain.com" \
  --enable-access-token-issuance true \
  --enable-id-token-issuance true \
  --public-client-redirect-uris "http://localhost:3000"

# Note the Application (client) ID
export FRONTEND_CLIENT_ID="<output-app-id>"

# Grant API permissions (frontend -> backend)
# Find the backend API permission ID
BACKEND_API_PERMISSION_ID=$(az ad app show --id $BACKEND_CLIENT_ID \
  --query "api.oauth2PermissionScopes[0].id" -o tsv)

# Add API permission to frontend
az ad app permission add \
  --id $FRONTEND_CLIENT_ID \
  --api $BACKEND_CLIENT_ID \
  --api-permissions $BACKEND_API_PERMISSION_ID=Scope

# Grant admin consent
az ad app permission grant \
  --id $FRONTEND_CLIENT_ID \
  --api $BACKEND_CLIENT_ID

# Also grant admin consent via admin-consent
az ad app permission admin-consent --id $FRONTEND_CLIENT_ID
```

**Record these values:**
```
Frontend Client ID: ___________________________
Redirect URIs:
  - http://localhost:3000 (dev)
  - https://dev.yourdomain.com (dev environment)
  - https://stage.yourdomain.com (stage environment)
  - https://yourdomain.com (production)
```

#### 1.3 Update Redirect URIs for All Environments

```bash
# Add all redirect URIs
az ad app update --id $FRONTEND_CLIENT_ID \
  --web-redirect-uris \
    "http://localhost:3000" \
    "https://dev-aksstarter.yourdomain.com" \
    "https://stage-aksstarter.yourdomain.com" \
    "https://aksstarter.yourdomain.com"
```

### Step 2: Create Service Principal for Terraform

```bash
# Create service principal with Contributor role
az ad sp create-for-rbac \
  --name "aksstarter-terraform-sp" \
  --role Contributor \
  --scopes /subscriptions/YOUR_SUBSCRIPTION_ID

# Output will look like:
# {
#   "appId": "xxxxx",
#   "displayName": "aksstarter-terraform-sp",
#   "password": "xxxxx",
#   "tenant": "xxxxx"
# }
```

**Record these values:**
```
Service Principal App ID: ___________________________
Service Principal Password: __________________________
Service Principal Tenant ID: _________________________
Subscription ID: _____________________________________
```

### Step 3: Create Terraform State Storage

```bash
# Variables
RESOURCE_GROUP="rg-tfstate-prod"
STORAGE_ACCOUNT="sttfstate$(date +%s)"  # Must be globally unique
CONTAINER_NAME="tfstate"
LOCATION="eastus"

# Create resource group
az group create \
  --name $RESOURCE_GROUP \
  --location $LOCATION

# Create storage account
az storage account create \
  --name $STORAGE_ACCOUNT \
  --resource-group $RESOURCE_GROUP \
  --location $LOCATION \
  --sku Standard_LRS \
  --encryption-services blob \
  --https-only true \
  --min-tls-version TLS1_2

# Create blob container
az storage container create \
  --name $CONTAINER_NAME \
  --account-name $STORAGE_ACCOUNT \
  --auth-mode login

# Enable versioning
az storage account blob-service-properties update \
  --account-name $STORAGE_ACCOUNT \
  --resource-group $RESOURCE_GROUP \
  --enable-versioning true
```

**Record these values:**
```
Terraform State Resource Group: ___________________________
Terraform State Storage Account: __________________________
Terraform State Container: tfstate
```

---

## Azure DevOps Setup

### Step 1: Create Azure DevOps Project

1. Go to https://dev.azure.com
2. Click **+ New Project**
3. Enter details:
   - **Project name**: `AKS-Starter-Kit`
   - **Visibility**: Private (recommended)
   - **Version control**: Git
   - **Work item process**: Agile
4. Click **Create**

### Step 2: Import Repository

#### Option A: Push from Local

```bash
# Clone this repository locally
git clone https://github.com/yourorg/aks-starter-kit.git
cd aks-starter-kit

# Add Azure DevOps remote
git remote add azure https://dev.azure.com/yourorg/AKS-Starter-Kit/_git/AKS-Starter-Kit

# Push code
git push azure main
```

#### Option B: Import in Azure DevOps UI

1. Go to **Repos** → **Files**
2. Click **Import**
3. Enter repository URL
4. Click **Import**

### Step 3: Create Service Connections

#### 3.1 Azure Resource Manager Service Connection

1. Go to **Project Settings** → **Service connections**
2. Click **New service connection**
3. Select **Azure Resource Manager**
4. Choose **Service principal (manual)**
5. Enter details:
   ```
   Subscription ID: <YOUR_SUBSCRIPTION_ID>
   Subscription Name: <YOUR_SUBSCRIPTION_NAME>
   Service Principal Id: <SP_APP_ID from Step 2>
   Service Principal Key: <SP_PASSWORD from Step 2>
   Tenant ID: <TENANT_ID from Step 2>
   ```
6. **Service connection name**: `azure-service-connection`
7. Check **Grant access permission to all pipelines**
8. Click **Verify and save**

#### 3.2 Azure Container Registry Service Connection

**Note**: This will be created after ACR is provisioned. For now, create a placeholder or skip and return after infrastructure deployment.

After infrastructure is deployed:

1. Click **New service connection**
2. Select **Docker Registry**
3. Choose **Azure Container Registry**
4. Select your subscription
5. Select your ACR (e.g., `acraksstarterprod`)
6. **Service connection name**: `acr-service-connection`
7. Click **Save**

#### 3.3 SonarQube Service Connection (Optional)

If you're using SonarQube:

1. Click **New service connection**
2. Select **SonarQube**
3. Enter:
   ```
   Server URL: https://your-sonarqube-server.com
   Token: <SONARQUBE_TOKEN>
   ```
4. **Service connection name**: `sonarqube-service-connection`
5. Click **Save**

### Step 4: Create Environments

1. Go to **Pipelines** → **Environments**
2. Click **New environment**

**Create three environments:**

#### Environment 1: dev
- **Name**: `dev`
- **Description**: Development environment
- **Approvals**: None
- Click **Create**

#### Environment 2: stage
- **Name**: `stage`
- **Description**: Staging environment
- **Approvals**:
  1. Click **Approvals and checks**
  2. Click **+** → **Approvals**
  3. Add approvers (team leads)
  4. Set **Minimum number of approvers**: 1
  5. Click **Create**

#### Environment 3: prod
- **Name**: `prod`
- **Description**: Production environment
- **Approvals**:
  1. Add approvers (senior engineers, managers)
  2. Set **Minimum number of approvers**: 2
  3. Add **Business Hours** check (optional)
  4. Add **Required template** check (optional)

### Step 5: Create Variable Groups

#### 5.1 Shared Variables

1. Go to **Pipelines** → **Library**
2. Click **+ Variable group**
3. **Name**: `shared-variables`
4. Add variables:
   ```
   projectName: aksstarter
   location: eastus
   acrName: acraksstarterprod
   tfstateResourceGroup: rg-tfstate-prod
   tfstateStorageAccount: <YOUR_STORAGE_ACCOUNT>
   tfstateContainer: tfstate
   ```
5. Click **Save**

#### 5.2 Dev Environment Variables

1. Create new variable group: `dev-variables`
2. Add variables:
   ```
   environment: dev
   azureSubscription: azure-service-connection
   resourceGroupName: rg-aksstarter-dev
   aksClusterName: aks-aksstarter-dev
   acrServiceConnection: acr-service-connection
   REACT_APP_CLIENT_ID: <FRONTEND_CLIENT_ID>
   REACT_APP_TENANT_ID: <TENANT_ID>
   REACT_APP_API_URL: https://dev-aksstarter.yourdomain.com/api
   REACT_APP_API_SCOPE: api://<BACKEND_CLIENT_ID>/access_as_user
   ```
3. Add secret variables (click lock icon):
   ```
   BACKEND_CLIENT_ID: <BACKEND_CLIENT_ID> (secret)
   TENANT_ID: <TENANT_ID> (secret)
   ```
4. Link to Azure Key Vault (optional but recommended):
   - Toggle **Link secrets from an Azure key vault**
   - Select subscription and Key Vault
   - Add secrets to import

#### 5.3 Stage Environment Variables

Create `stage-variables` with similar structure but stage-specific values.

#### 5.4 Prod Environment Variables

Create `prod-variables` with production-specific values.

### Step 6: Create Pipeline Variables for Secrets

For sensitive values, use Azure DevOps secure files or pipeline variables:

1. Go to **Pipelines** → **Library** → **Secure files**
2. Upload any required certificates or key files
3. Or use pipeline variables marked as secret

---

## Pipeline Configuration

### Step 1: Create Infrastructure Pipeline

1. Go to **Pipelines** → **Pipelines**
2. Click **New pipeline**
3. Select **Azure Repos Git**
4. Select your repository
5. Choose **Existing Azure Pipelines YAML file**
6. Path: `/azure-pipelines/infra-deploy.yml`
7. Click **Continue**
8. Review the pipeline
9. Click **Save** (don't run yet)
10. Rename pipeline to **"Infrastructure Deployment"**

### Step 2: Create Backend Build Pipeline

1. Click **New pipeline**
2. Select repository and existing YAML file
3. Path: `/azure-pipelines/build-backend.yml`
4. Save and rename to **"Backend - Build & Test"**

### Step 3: Create Frontend Build Pipeline

1. Click **New pipeline**
2. Select repository and existing YAML file
3. Path: `/azure-pipelines/build-frontend.yml`
4. Save and rename to **"Frontend - Build & Test"**

### Step 4: Create Release Pipeline

1. Click **New pipeline**
2. Select repository and existing YAML file
3. Path: `/azure-pipelines/release-deploy.yml`
4. Save and rename to **"Release - Deploy to AKS"**

### Step 5: Configure Pipeline Permissions

For each pipeline:
1. Click **Edit**
2. Click **⋮** (three dots) → **Manage security**
3. Grant permissions to service connections
4. Grant permissions to environments

---

## First Deployment

### Phase 1: Deploy Infrastructure (Dev Environment)

#### Step 1: Update Terraform Variables

Edit `infra/envs/dev/terraform.tfvars`:

```hcl
project_name = "aksstarter"
environment  = "dev"
location     = "eastus"

# Networking
aks_vnet_address_space = ["10.1.0.0/16"]
aks_subnet_address_prefix = "10.1.1.0/24"
appgw_subnet_address_prefix = "10.1.2.0/24"

# AKS Configuration
aks_node_count = 2
aks_node_size  = "Standard_D2s_v3"
kubernetes_version = "1.28.0"

# Tags
tags = {
  Environment = "dev"
  Project     = "AKS Starter Kit"
  ManagedBy   = "Terraform"
  CostCenter  = "Engineering"
}
```

#### Step 2: Create Backend Configuration

Create `infra/envs/dev/backend-dev.hcl`:

```hcl
resource_group_name  = "rg-tfstate-prod"
storage_account_name = "<YOUR_STORAGE_ACCOUNT>"
container_name       = "tfstate"
key                  = "dev/terraform.tfstate"
```

Commit and push:

```bash
git add infra/envs/dev/terraform.tfvars infra/envs/dev/backend-dev.hcl
git commit -m "Configure dev environment variables"
git push azure main
```

#### Step 3: Run Infrastructure Pipeline

1. Go to **Pipelines** → **Infrastructure Deployment**
2. Click **Run pipeline**
3. Select branch: `main`
4. Set variable: `environment = dev`
5. Click **Run**

**Pipeline will:**
- ✓ Initialize Terraform with remote backend
- ✓ Run `terraform plan`
- ✓ Wait for manual approval
- ✓ Run `terraform apply`
- ✓ Output resource details

**Expected Duration**: 20-30 minutes

#### Step 4: Verify Infrastructure

```bash
# Login to Azure
az login

# Set subscription
az account set --subscription "YOUR_SUBSCRIPTION_ID"

# List resources
az resource list \
  --resource-group rg-aksstarter-dev \
  --output table

# Get AKS credentials
az aks get-credentials \
  --resource-group rg-aksstarter-dev \
  --name aks-aksstarter-dev

# Verify cluster
kubectl get nodes
kubectl get namespaces
```

Expected resources:
- ✓ Resource Group: `rg-aksstarter-dev`
- ✓ AKS Cluster: `aks-aksstarter-dev`
- ✓ Azure Container Registry: `acraksstarterprod`
- ✓ Key Vault: `kv-aksstarter-dev-xxxxx`
- ✓ Application Gateway: `appgw-aksstarter-dev`
- ✓ Log Analytics Workspace: `law-aksstarter-dev`
- ✓ Application Insights: `ai-aksstarter-dev`

### Phase 2: Build and Push Container Images

#### Step 1: Run Backend Build Pipeline

1. Go to **Pipelines** → **Backend - Build & Test**
2. Click **Run pipeline**
3. Select branch: `main`
4. Click **Run**

**Pipeline will:**
- ✓ Run Checkstyle validation
- ✓ Check code formatting (Spotless)
- ✓ Run Maven build
- ✓ Execute unit tests
- ✓ Run integration tests (Cucumber BDD)
- ✓ Generate code coverage report
- ✓ Run SonarQube analysis (if configured)
- ✓ Build Docker image
- ✓ Scan with Trivy
- ✓ Push to ACR with tag `$(Build.BuildId)` and `latest`

**Expected Duration**: 5-10 minutes

#### Step 2: Run Frontend Build Pipeline

1. Go to **Pipelines** → **Frontend - Build & Test**
2. Click **Run pipeline**
3. Select branch: `main`
4. Click **Run**

**Pipeline will:**
- ✓ Install npm dependencies
- ✓ Run unit tests (Jest)
- ✓ Generate code coverage
- ✓ Build production bundle
- ✓ Run E2E tests (Cypress) - optional
- ✓ Build Docker image
- ✓ Scan with Trivy
- ✓ Push to ACR

**Expected Duration**: 3-5 minutes

#### Step 3: Verify Container Images in ACR

```bash
# List repositories
az acr repository list \
  --name acraksstarterprod \
  --output table

# List tags for backend
az acr repository show-tags \
  --name acraksstarterprod \
  --repository aks-starter-backend \
  --output table

# List tags for frontend
az acr repository show-tags \
  --name acraksstarterprod \
  --repository aks-starter-frontend \
  --output table
```

Expected output:
```
Repository               Tags
-----------------------  ----------------
aks-starter-backend      latest, 1, 2, 3
aks-starter-frontend     latest, 1, 2, 3
```

### Phase 3: Deploy Applications to AKS

#### Step 1: Update Helm Values for Dev

Edit `charts/backend/values-dev.yaml`:

```yaml
image:
  repository: acraksstarterprod.azurecr.io/aks-starter-backend
  tag: latest
  pullPolicy: Always

ingress:
  enabled: true
  hosts:
    - host: dev-aksstarter-api.yourdomain.com
      paths:
        - path: /
          pathType: Prefix

env:
  - name: SPRING_PROFILES_ACTIVE
    value: "dev"
  - name: AZURE_CLIENT_ID
    valueFrom:
      secretKeyRef:
        name: azure-ad-credentials
        key: client-id
  - name: AZURE_TENANT_ID
    valueFrom:
      secretKeyRef:
        name: azure-ad-credentials
        key: tenant-id
```

Edit `charts/frontend/values-dev.yaml`:

```yaml
image:
  repository: acraksstarterprod.azurecr.io/aks-starter-frontend
  tag: latest

ingress:
  hosts:
    - host: dev-aksstarter.yourdomain.com
      paths:
        - path: /
          pathType: Prefix

env:
  - name: REACT_APP_CLIENT_ID
    value: "<FRONTEND_CLIENT_ID>"
  - name: REACT_APP_TENANT_ID
    value: "<TENANT_ID>"
  - name: REACT_APP_API_URL
    value: "https://dev-aksstarter-api.yourdomain.com"
  - name: REACT_APP_API_SCOPE
    value: "api://<BACKEND_CLIENT_ID>/access_as_user"
```

Commit and push:

```bash
git add charts/*/values-dev.yaml
git commit -m "Configure Helm values for dev environment"
git push azure main
```

#### Step 2: Create Kubernetes Secrets

```bash
# Get AKS credentials
az aks get-credentials \
  --resource-group rg-aksstarter-dev \
  --name aks-aksstarter-dev

# Create namespace
kubectl create namespace dev

# Create Azure AD credentials secret
kubectl create secret generic azure-ad-credentials \
  --from-literal=client-id="<BACKEND_CLIENT_ID>" \
  --from-literal=tenant-id="<TENANT_ID>" \
  --namespace dev

# Create image pull secret for ACR
kubectl create secret docker-registry acr-secret \
  --docker-server=acraksstarterprod.azurecr.io \
  --docker-username=<ACR_USERNAME> \
  --docker-password=<ACR_PASSWORD> \
  --namespace dev

# Or use managed identity (recommended)
az aks update \
  --resource-group rg-aksstarter-dev \
  --name aks-aksstarter-dev \
  --attach-acr acraksstarterprod
```

#### Step 3: Run Release Pipeline

1. Go to **Pipelines** → **Release - Deploy to AKS**
2. Click **Run pipeline**
3. Set variables:
   ```
   environment: dev
   backendImageTag: latest
   frontendImageTag: latest
   ```
4. Click **Run**

**Pipeline will:**
- ✓ Download artifacts
- ✓ Get AKS credentials
- ✓ Deploy backend with Helm
- ✓ Deploy frontend with Helm
- ✓ Wait for rollout completion
- ✓ Run smoke tests
- ✓ Publish deployment results

**Expected Duration**: 3-5 minutes

#### Step 4: Verify Deployment

```bash
# Check deployments
kubectl get deployments -n dev
kubectl get pods -n dev
kubectl get services -n dev
kubectl get ingress -n dev

# Check pod logs
kubectl logs -n dev deployment/backend -f
kubectl logs -n dev deployment/frontend -f

# Check pod status
kubectl describe pod -n dev -l app=backend
kubectl describe pod -n dev -l app=frontend
```

Expected output:
```
NAME                        READY   STATUS    RESTARTS   AGE
backend-7d4b9c5f8d-xxxxx    1/1     Running   0          2m
backend-7d4b9c5f8d-xxxxx    1/1     Running   0          2m
backend-7d4b9c5f8d-xxxxx    1/1     Running   0          2m
frontend-6b5c8d9f7e-xxxxx   1/1     Running   0          2m
frontend-6b5c8d9f7e-xxxxx   1/1     Running   0          2m
```

#### Step 5: Configure DNS (if using custom domain)

```bash
# Get Application Gateway public IP
az network public-ip show \
  --resource-group rg-aksstarter-dev \
  --name pip-appgw-dev \
  --query ipAddress \
  --output tsv

# Example output: 20.123.45.67

# Create DNS A records:
# dev-aksstarter.yourdomain.com -> 20.123.45.67
# dev-aksstarter-api.yourdomain.com -> 20.123.45.67
```

In your DNS provider (e.g., Azure DNS, GoDaddy, Cloudflare):
```
Type    Name                              Value
----    ----                              -----
A       dev-aksstarter                    20.123.45.67
A       dev-aksstarter-api                20.123.45.67
```

#### Step 6: Test the Application

```bash
# Test backend health
curl https://dev-aksstarter-api.yourdomain.com/actuator/health

# Test backend API (requires auth)
curl https://dev-aksstarter-api.yourdomain.com/api/hello \
  -H "Authorization: Bearer <ACCESS_TOKEN>"

# Open frontend in browser
open https://dev-aksstarter.yourdomain.com
```

**Manual Testing:**
1. Open `https://dev-aksstarter.yourdomain.com`
2. Click **Login**
3. Sign in with Azure AD
4. Verify successful login
5. Click **Call API** button
6. Verify API response is displayed

---

## Deployment Flow

### Complete CI/CD Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                         CODE CHANGES                                 │
│                                                                      │
│  Developer commits code to feature branch                            │
│         │                                                            │
│         ├─► git commit -m "Add new feature"                         │
│         └─► git push origin feature/new-feature                     │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      PULL REQUEST (PR)                               │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────┐    │
│  │  PR Build Validation (Triggered automatically)              │    │
│  │                                                             │    │
│  │  Frontend Pipeline:                                         │    │
│  │    1. npm install                                           │    │
│  │    2. npm run lint                                          │    │
│  │    3. npm test (unit tests)                                 │    │
│  │    4. npm run build                                         │    │
│  │    5. Code coverage check (70% threshold)                   │    │
│  │                                                             │    │
│  │  Backend Pipeline:                                          │    │
│  │    1. mvn checkstyle:check                                  │    │
│  │    2. mvn spotless:check                                    │    │
│  │    3. mvn clean test (unit tests)                           │    │
│  │    4. mvn verify (integration tests + BDD)                  │    │
│  │    5. JaCoCo coverage check (70% threshold)                 │    │
│  │    6. SonarQube analysis (quality gate)                     │    │
│  │                                                             │    │
│  │  Status: ✓ All checks passed                               │    │
│  └────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  Code Review → Approval → Merge to main                             │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    MAIN BRANCH BUILD                                 │
│                                                                      │
│  Trigger: Merge to main or manual trigger                           │
│                                                                      │
│  ┌─────────────────────┐        ┌─────────────────────┐            │
│  │  Frontend Build      │        │  Backend Build       │            │
│  │  Pipeline            │        │  Pipeline            │            │
│  └──────────┬───────────┘        └──────────┬───────────┘            │
│             │                               │                        │
│             ▼                               ▼                        │
│  ┌─────────────────────┐        ┌─────────────────────┐            │
│  │  1. Code Quality     │        │  1. Checkstyle       │            │
│  │  2. Unit Tests       │        │  2. Spotless         │            │
│  │  3. Build            │        │  3. Unit Tests       │            │
│  │  4. E2E Tests (opt)  │        │  4. BDD Tests        │            │
│  │  5. Docker Build     │        │  5. Docker Build     │            │
│  │  6. Trivy Scan       │        │  6. Trivy Scan       │            │
│  │  7. Push to ACR      │        │  7. Push to ACR      │            │
│  └──────────┬───────────┘        └──────────┬───────────┘            │
│             │                               │                        │
│             └───────────┬───────────────────┘                        │
│                         │                                            │
│                         ▼                                            │
│              ┌─────────────────────┐                                 │
│              │  Artifacts Created   │                                 │
│              │                      │                                 │
│              │  • Docker images     │                                 │
│              │  • Test reports      │                                 │
│              │  • Coverage reports  │                                 │
│              │  • Security scans    │                                 │
│              └──────────┬───────────┘                                 │
└──────────────────────────┼──────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────────┐
│                  RELEASE PIPELINE (CD)                               │
│                                                                      │
│  Trigger: Successful build or manual release                        │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                      DEV DEPLOYMENT                          │   │
│  │                                                              │   │
│  │  1. Get AKS credentials (aks-aksstarter-dev)                │   │
│  │  2. Deploy backend (Helm upgrade --install)                 │   │
│  │     - Image: acr.azurecr.io/backend:${BUILD_ID}             │   │
│  │     - Namespace: dev                                         │   │
│  │     - Values: values-dev.yaml                                │   │
│  │  3. Deploy frontend (Helm upgrade --install)                │   │
│  │     - Image: acr.azurecr.io/frontend:${BUILD_ID}            │   │
│  │  4. Wait for rollout (kubectl rollout status)               │   │
│  │  5. Run smoke tests                                          │   │
│  │     ✓ Health endpoint check                                  │   │
│  │     ✓ API connectivity test                                  │   │
│  │  6. Publish deployment logs                                  │   │
│  │                                                              │   │
│  │  Status: ✓ Deployed to dev                                  │   │
│  └──────────────────────────────┬───────────────────────────────┘   │
│                                 │                                   │
│                                 ▼                                   │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                    STAGE DEPLOYMENT                          │   │
│  │                                                              │   │
│  │  Pre-Deployment:                                             │   │
│  │    ⏸  Manual Approval Required (1 approver)                 │   │
│  │    ⏸  Business hours check (optional)                       │   │
│  │                                                              │   │
│  │  Deployment (same steps as dev):                            │   │
│  │    1. Get AKS credentials (aks-aksstarter-stage)            │   │
│  │    2. Deploy backend (namespace: stage)                     │   │
│  │    3. Deploy frontend                                        │   │
│  │    4. Wait for rollout                                       │   │
│  │    5. Run smoke tests + integration tests                   │   │
│  │    6. Run security scan (OWASP ZAP baseline)                │   │
│  │                                                              │   │
│  │  Status: ✓ Deployed to stage                                │   │
│  └──────────────────────────────┬───────────────────────────────┘   │
│                                 │                                   │
│                                 ▼                                   │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                     PROD DEPLOYMENT                          │   │
│  │                                                              │   │
│  │  Pre-Deployment:                                             │   │
│  │    ⏸  Manual Approval Required (2 approvers)                │   │
│  │    ⏸  Business hours check                                  │   │
│  │    ⏸  Change management ticket validation                   │   │
│  │                                                              │   │
│  │  Deployment Strategy: Blue-Green (optional)                 │   │
│  │    1. Get AKS credentials (aks-aksstarter-prod)             │   │
│  │    2. Deploy backend (namespace: prod)                      │   │
│  │       - HPA: min 3, max 15 replicas                         │   │
│  │       - PDB: minAvailable 2                                  │   │
│  │    3. Deploy frontend                                        │   │
│  │       - HPA: min 3, max 10 replicas                         │   │
│  │    4. Health check wait (2 minutes)                         │   │
│  │    5. Run smoke tests                                        │   │
│  │    6. Monitor for 5 minutes                                  │   │
│  │    7. Send deployment notification (Teams/Email)            │   │
│  │                                                              │   │
│  │  Rollback Plan:                                              │   │
│  │    - If smoke tests fail → Automatic rollback               │   │
│  │    - If monitoring alerts → Manual rollback option          │   │
│  │                                                              │   │
│  │  Status: ✓ Deployed to production                           │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    POST-DEPLOYMENT                                   │
│                                                                      │
│  1. ✓ Application Insights monitoring                               │
│  2. ✓ Log aggregation in Log Analytics                              │
│  3. ✓ Prometheus metrics collection                                 │
│  4. ✓ Azure Monitor alerts (errors, performance)                    │
│  5. ✓ Deployment notification sent                                  │
│  6. ✓ Documentation updated                                         │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Infrastructure Deployment Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                  INFRASTRUCTURE PIPELINE                             │
│                                                                      │
│  Trigger: Manual or scheduled                                       │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Stage 1: Terraform Init & Plan                             │   │
│  │                                                              │   │
│  │  1. Checkout code                                            │   │
│  │  2. Setup Terraform                                          │   │
│  │  3. terraform init                                           │   │
│  │     - Backend: Azure Storage (tfstate)                      │   │
│  │     - State file: ${env}/terraform.tfstate                  │   │
│  │  4. terraform validate                                       │   │
│  │  5. terraform plan -out=tfplan                               │   │
│  │  6. Publish plan as artifact                                 │   │
│  │                                                              │   │
│  │  Resources to be created:                                    │   │
│  │    • Resource Group                                          │   │
│  │    • Virtual Network (with subnets)                          │   │
│  │    • AKS Cluster                                             │   │
│  │    • Azure Container Registry                                │   │
│  │    • Key Vault                                               │   │
│  │    • Application Gateway + WAF                               │   │
│  │    • API Management                                          │   │
│  │    • Log Analytics Workspace                                 │   │
│  │    • Application Insights                                    │   │
│  │    • Managed Identities                                      │   │
│  │    • Role Assignments                                        │   │
│  └──────────────────────────────┬───────────────────────────────┘   │
│                                 │                                   │
│                                 ▼                                   │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Stage 2: Security & Compliance Checks                       │   │
│  │                                                              │   │
│  │  1. Run tfsec (Terraform security scanner)                  │   │
│  │  2. Run tflint (Terraform linter)                           │   │
│  │  3. Check for compliance violations                         │   │
│  │  4. Validate naming conventions                             │   │
│  │  5. Publish security scan results                           │   │
│  └──────────────────────────────┬───────────────────────────────┘   │
│                                 │                                   │
│                                 ▼                                   │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Stage 3: Manual Approval                                    │   │
│  │                                                              │   │
│  │  ⏸  Review Terraform plan                                   │   │
│  │  ⏸  Verify estimated costs                                  │   │
│  │  ⏸  Approve or reject deployment                            │   │
│  └──────────────────────────────┬───────────────────────────────┘   │
│                                 │                                   │
│                                 ▼                                   │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Stage 4: Terraform Apply                                    │   │
│  │                                                              │   │
│  │  1. Download tfplan artifact                                 │   │
│  │  2. terraform apply tfplan                                   │   │
│  │  3. Wait for resource provisioning (20-30 min)              │   │
│  │  4. terraform output > outputs.json                          │   │
│  │  5. Store outputs as pipeline variables                     │   │
│  │                                                              │   │
│  │  Outputs:                                                    │   │
│  │    • aks_cluster_name                                        │   │
│  │    • acr_login_server                                        │   │
│  │    • key_vault_uri                                           │   │
│  │    • app_gateway_public_ip                                   │   │
│  │    • log_analytics_workspace_id                              │   │
│  └──────────────────────────────┬───────────────────────────────┘   │
│                                 │                                   │
│                                 ▼                                   │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Stage 5: Post-Deployment Configuration                      │   │
│  │                                                              │   │
│  │  1. Configure kubectl context                               │   │
│  │  2. Install cert-manager (for TLS)                          │   │
│  │  3. Install ingress-nginx (if using)                        │   │
│  │  4. Configure Key Vault CSI driver                          │   │
│  │  5. Set up cluster autoscaler                               │   │
│  │  6. Configure monitoring (Prometheus operator)              │   │
│  │  7. Create namespaces (dev, stage, prod)                    │   │
│  │  8. Apply resource quotas                                    │   │
│  │                                                              │   │
│  │  Status: ✓ Infrastructure ready                             │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Troubleshooting

### Common Issues and Solutions

#### Issue 1: Pipeline Fails at Authentication

**Symptom:**
```
ERROR: Failed to authenticate with Azure
```

**Solution:**
1. Verify service connection is valid
2. Check service principal hasn't expired
3. Verify subscription ID is correct
4. Re-create service connection if needed

#### Issue 2: Terraform State Lock

**Symptom:**
```
Error: Error acquiring the state lock
```

**Solution:**
```bash
# Force unlock (use with caution)
cd infra/envs/dev
terraform force-unlock <LOCK_ID>
```

#### Issue 3: ACR Authentication Failed

**Symptom:**
```
Error: unauthorized: authentication required
```

**Solution:**
```bash
# Attach ACR to AKS
az aks update \
  --resource-group rg-aksstarter-dev \
  --name aks-aksstarter-dev \
  --attach-acr acraksstarterprod

# Or create image pull secret
kubectl create secret docker-registry acr-secret \
  --docker-server=acraksstarterprod.azurecr.io \
  --docker-username=<USERNAME> \
  --docker-password=<PASSWORD> \
  --namespace dev
```

#### Issue 4: Pods Not Starting

**Symptom:**
```
kubectl get pods shows ImagePullBackOff or CrashLoopBackOff
```

**Solution:**
```bash
# Check pod logs
kubectl logs -n dev pod/<POD_NAME>

# Check pod events
kubectl describe pod -n dev <POD_NAME>

# Common fixes:
# 1. Wrong image tag
helm upgrade backend ./charts/backend \
  --set image.tag=<CORRECT_TAG> \
  --namespace dev

# 2. Missing secrets
kubectl get secrets -n dev

# 3. Resource limits too low
kubectl describe pod -n dev <POD_NAME> | grep -A 5 "Limits"
```

#### Issue 5: Ingress Not Working

**Symptom:**
```
502 Bad Gateway or 404 Not Found
```

**Solution:**
```bash
# Check ingress
kubectl get ingress -n dev
kubectl describe ingress -n dev

# Check Application Gateway backend health
az network application-gateway show-backend-health \
  --resource-group rg-aksstarter-dev \
  --name appgw-aksstarter-dev

# Verify DNS
nslookup dev-aksstarter.yourdomain.com

# Check TLS certificate
kubectl get certificate -n dev
```

#### Issue 6: Build Pipeline Fails at Tests

**Symptom:**
```
Tests failed: Expected 201 but got 401
```

**Solution:**
1. Check if test database is configured
2. Verify test configuration in application-test.yml
3. Check for port conflicts
4. Review test logs in pipeline artifacts

#### Issue 7: Helm Upgrade Fails

**Symptom:**
```
Error: UPGRADE FAILED: another operation (install/upgrade/rollback) is in progress
```

**Solution:**
```bash
# Check pending operations
helm list -n dev --pending

# Rollback if needed
helm rollback backend -n dev

# Force delete if stuck
helm delete backend -n dev --no-hooks
```

### Getting Help

1. **Check Pipeline Logs**: Download logs from Azure DevOps
2. **Check Kubernetes Events**: `kubectl get events -n dev --sort-by='.lastTimestamp'`
3. **Check Application Logs**: `kubectl logs -n dev -l app=backend --tail=100`
4. **Check Azure Portal**: Monitor → Logs → Run KQL queries
5. **Review Documentation**: Check QUICKSTART.md and architecture.md

---

## Summary Checklist

### Azure Setup
- [ ] Azure AD app registrations created (frontend + backend)
- [ ] Service principal created for Terraform
- [ ] Terraform state storage created
- [ ] DNS zones configured (if using custom domain)

### Azure DevOps Setup
- [ ] Project created
- [ ] Code repository imported
- [ ] Service connections created (ARM, ACR, SonarQube)
- [ ] Environments created (dev, stage, prod)
- [ ] Variable groups created
- [ ] Pipelines created (4 pipelines)

### Infrastructure
- [ ] Terraform variables updated
- [ ] Infrastructure pipeline run successfully
- [ ] All Azure resources created
- [ ] AKS cluster accessible

### Applications
- [ ] Backend build pipeline successful
- [ ] Frontend build pipeline successful
- [ ] Container images pushed to ACR
- [ ] Helm values updated
- [ ] Kubernetes secrets created
- [ ] Applications deployed to AKS

### Testing
- [ ] Backend health endpoint responds
- [ ] Frontend loads in browser
- [ ] Authentication works
- [ ] API calls successful
- [ ] Monitoring shows data

---

## Next Steps

After successful deployment:

1. **Set up monitoring dashboards** - Create custom Azure Monitor workbooks
2. **Configure alerts** - Set up alerts for errors, performance, costs
3. **Enable backups** - Configure backup for critical data
4. **Document runbooks** - Create operational procedures
5. **Train team** - Ensure team can operate and maintain the solution
6. **Plan DR** - Test disaster recovery procedures
7. **Optimize costs** - Review and optimize Azure resource sizes

For customization guidance, see **CHECKLIST.md**.

For architecture details, see **ARCHITECTURE_DETAILED.md**.
