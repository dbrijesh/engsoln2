# GitHub Actions Setup Guide

## Complete Guide: Setting up CI/CD with GitHub Actions

This guide walks you through setting up GitHub Actions workflows to build, test, and deploy the AKS Starter Kit to Azure.

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Azure Setup](#azure-setup)
3. [GitHub Repository Setup](#github-repository-setup)
4. [GitHub Secrets Configuration](#github-secrets-configuration)
5. [GitHub Variables Configuration](#github-variables-configuration)
6. [GitHub Environments Setup](#github-environments-setup)
7. [First Deployment](#first-deployment)
8. [Workflow Overview](#workflow-overview)
9. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Tools

- Azure CLI (`az`) - [Install](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli)
- Azure subscription with Owner or Contributor access
- GitHub account
- Git installed locally

### Required Knowledge

- Basic understanding of Azure services
- Git version control
- YAML syntax
- GitHub Actions basics
- Kubernetes basics

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
  --enable-access-token-issuance true

# Note the Application (client) ID
export BACKEND_CLIENT_ID="<output-app-id>"

# Expose an API scope
az ad app update --id $BACKEND_CLIENT_ID \
  --identifier-uris "api://$BACKEND_CLIENT_ID"

# Create service principal
az ad sp create --id $BACKEND_CLIENT_ID
```

**Record this value:**
```
Backend Client ID: ___________________________
```

#### 1.2 Register Frontend SPA Application

```bash
# Register the frontend SPA app
az ad app create \
  --display-name "aksstarter-frontend-spa" \
  --sign-in-audience "AzureADMyOrg" \
  --web-redirect-uris "http://localhost:3000" "https://yourdomain.com" \
  --enable-access-token-issuance true \
  --enable-id-token-issuance true

export FRONTEND_CLIENT_ID="<output-app-id>"

# Grant API permissions (frontend -> backend)
BACKEND_API_PERMISSION_ID=$(az ad app show --id $BACKEND_CLIENT_ID \
  --query "api.oauth2PermissionScopes[0].id" -o tsv)

az ad app permission add \
  --id $FRONTEND_CLIENT_ID \
  --api $BACKEND_CLIENT_ID \
  --api-permissions $BACKEND_API_PERMISSION_ID=Scope

az ad app permission admin-consent --id $FRONTEND_CLIENT_ID
```

**Record this value:**
```
Frontend Client ID: ___________________________
```

### Step 2: Create Service Principal for GitHub Actions

```bash
# Get your subscription ID
SUBSCRIPTION_ID=$(az account show --query id -o tsv)

# Create service principal with Contributor role
az ad sp create-for-rbac \
  --name "github-actions-sp" \
  --role Contributor \
  --scopes /subscriptions/$SUBSCRIPTION_ID \
  --sdk-auth

# Output will be JSON - SAVE THIS ENTIRE OUTPUT for AZURE_CREDENTIALS secret
```

**Record the entire JSON output:**
```json
{
  "clientId": "xxxxx",
  "clientSecret": "xxxxx",
  "subscriptionId": "xxxxx",
  "tenantId": "xxxxx",
  ...
}
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

### Step 4: Create Azure Container Registry (ACR)

```bash
# Variables
ACR_NAME="acraksstarter$(date +%s)"  # Must be globally unique
ACR_RG="rg-acr-shared"

# Create resource group
az group create --name $ACR_RG --location $LOCATION

# Create ACR
az acr create \
  --name $ACR_NAME \
  --resource-group $ACR_RG \
  --sku Premium \
  --admin-enabled true

# Get ACR credentials
ACR_USERNAME=$(az acr credential show --name $ACR_NAME --query username -o tsv)
ACR_PASSWORD=$(az acr credential show --name $ACR_NAME --query passwords[0].value -o tsv)
ACR_LOGIN_SERVER=$(az acr show --name $ACR_NAME --query loginServer -o tsv)
```

**Record these values:**
```
ACR Name: ___________________________
ACR Login Server: ___________________________
ACR Username: ___________________________
ACR Password: ___________________________
```

---

## GitHub Repository Setup

### Step 1: Create GitHub Repository (if not already exists)

1. Go to https://github.com
2. Click **New repository**
3. Enter repository name: `engsoln2`
4. Choose visibility (Public or Private)
5. Click **Create repository**

### Step 2: Clone and Push Code

```bash
# Navigate to your local project directory
cd /path/to/engchallenge

# Initialize git (if not already initialized)
git init

# Add all files
git add .

# Create initial commit
git commit -m "Initial commit: AKS Starter Kit with GitHub Actions"

# Add remote
git remote add origin https://github.com/dbrijesh/engsoln2.git

# Push to GitHub
git branch -M main
git push -u origin main
```

---

## GitHub Secrets Configuration

### Step 1: Navigate to Repository Settings

1. Go to your GitHub repository
2. Click **Settings**
3. Click **Secrets and variables** → **Actions**
4. Click **New repository secret**

### Step 2: Add Required Secrets

Add the following secrets one by one:

#### Azure Credentials

**Name:** `AZURE_CREDENTIALS`
**Value:** (Paste the entire JSON output from service principal creation)
```json
{
  "clientId": "xxxxx",
  "clientSecret": "xxxxx",
  "subscriptionId": "xxxxx",
  "tenantId": "xxxxx",
  "activeDirectoryEndpointUrl": "https://login.microsoftonline.com",
  "resourceManagerEndpointUrl": "https://management.azure.com/",
  "activeDirectoryGraphResourceId": "https://graph.windows.net/",
  "sqlManagementEndpointUrl": "https://management.core.windows.net:8443/",
  "galleryEndpointUrl": "https://gallery.azure.com/",
  "managementEndpointUrl": "https://management.core.windows.net/"
}
```

#### Individual ARM Credentials (extracted from AZURE_CREDENTIALS)

**Name:** `ARM_CLIENT_ID`
**Value:** `<clientId from service principal>`

**Name:** `ARM_CLIENT_SECRET`
**Value:** `<clientSecret from service principal>`

**Name:** `ARM_SUBSCRIPTION_ID`
**Value:** `<subscriptionId from service principal>`

**Name:** `ARM_TENANT_ID`
**Value:** `<tenantId from service principal>`

#### Terraform State Configuration

**Name:** `TF_STATE_RESOURCE_GROUP`
**Value:** `rg-tfstate-prod`

**Name:** `TF_STATE_STORAGE_ACCOUNT`
**Value:** `<your storage account name>`

**Name:** `TF_STATE_CONTAINER`
**Value:** `tfstate`

#### Azure Container Registry

**Name:** `ACR_LOGIN_SERVER`
**Value:** `<your-acr-name>.azurecr.io`

**Name:** `ACR_USERNAME`
**Value:** `<ACR username>`

**Name:** `ACR_PASSWORD`
**Value:** `<ACR password>`

#### Azure AD Application IDs

**Name:** `BACKEND_CLIENT_ID`
**Value:** `<Backend API Application ID>`

**Name:** `REACT_APP_CLIENT_ID`
**Value:** `<Frontend SPA Application ID>`

**Name:** `REACT_APP_TENANT_ID`
**Value:** `<Azure AD Tenant ID>`

**Name:** `REACT_APP_API_URL`
**Value:** `https://dev-api-yourdomain.com` (update per environment)

**Name:** `REACT_APP_API_SCOPE`
**Value:** `api://<BACKEND_CLIENT_ID>/access_as_user`

#### SonarQube (Optional)

**Name:** `SONAR_TOKEN`
**Value:** `<SonarQube token>` (if using SonarQube)

### Summary of Required Secrets

```
✓ AZURE_CREDENTIALS (JSON)
✓ ARM_CLIENT_ID
✓ ARM_CLIENT_SECRET
✓ ARM_SUBSCRIPTION_ID
✓ ARM_TENANT_ID
✓ TF_STATE_RESOURCE_GROUP
✓ TF_STATE_STORAGE_ACCOUNT
✓ TF_STATE_CONTAINER
✓ ACR_LOGIN_SERVER
✓ ACR_USERNAME
✓ ACR_PASSWORD
✓ BACKEND_CLIENT_ID
✓ REACT_APP_CLIENT_ID
✓ REACT_APP_TENANT_ID
✓ REACT_APP_API_URL
✓ REACT_APP_API_SCOPE
✓ SONAR_TOKEN (optional)
```

---

## GitHub Variables Configuration

Variables are non-sensitive configuration values.

### Step 1: Navigate to Variables

1. Go to **Settings** → **Secrets and variables** → **Actions**
2. Click **Variables** tab
3. Click **New repository variable**

### Step 2: Add Required Variables

**Name:** `SONAR_HOST_URL`
**Value:** `https://your-sonarqube-server.com` (if using SonarQube, otherwise leave empty)

**Name:** `DOMAIN_NAME`
**Value:** `yourdomain.com`

**Name:** `AKS_RESOURCE_GROUP_DEV`
**Value:** `rg-aksstarter-dev`

**Name:** `AKS_CLUSTER_NAME_DEV`
**Value:** `aks-aksstarter-dev`

**Name:** `AKS_RESOURCE_GROUP_STAGE`
**Value:** `rg-aksstarter-stage`

**Name:** `AKS_CLUSTER_NAME_STAGE`
**Value:** `aks-aksstarter-stage`

**Name:** `AKS_RESOURCE_GROUP_PROD`
**Value:** `rg-aksstarter-prod`

**Name:** `AKS_CLUSTER_NAME_PROD`
**Value:** `aks-aksstarter-prod`

---

## GitHub Environments Setup

Environments provide deployment protection rules and environment-specific secrets.

### Step 1: Create Environments

1. Go to **Settings** → **Environments**
2. Click **New environment**

### Step 2: Create 'dev' Environment

**Name:** `dev`

**Protection rules:**
- No required reviewers
- No wait timer
- No deployment branches (allow all)

Click **Save protection rules**

### Step 3: Create 'stage' Environment

**Name:** `stage`

**Protection rules:**
- ✓ Required reviewers: Add 1-2 team members
- ✓ Wait timer: 0 minutes (or set delay if needed)
- Deployment branches: Only `main` branch

Click **Save protection rules**

### Step 4: Create 'prod' Environment

**Name:** `prod`

**Protection rules:**
- ✓ Required reviewers: Add 2+ senior team members
- ✓ Wait timer: 0 minutes
- Deployment branches: Only `main` branch
- (Optional) Add custom protection rules

Click **Save protection rules**

### Step 5: Create 'prod-destroy' Environment

**Name:** `prod-destroy`

**Protection rules:**
- ✓ Required reviewers: Add all senior team members
- ✓ Prevent self-review
- This environment is for terraform destroy operations

---

## First Deployment

### Phase 1: Update Terraform Variables

Edit `infra/envs/dev/terraform.tfvars`:

```hcl
project_name = "aksstarter"
environment  = "dev"
location     = "eastus"

# Update these with your values
backend_client_id = "<BACKEND_CLIENT_ID>"
tenant_id         = "<ARM_TENANT_ID>"

# Networking
aks_vnet_address_space      = ["10.1.0.0/16"]
aks_subnet_address_prefix   = "10.1.1.0/24"
appgw_subnet_address_prefix = "10.1.2.0/24"

# AKS Configuration
aks_node_count     = 2
aks_node_size      = "Standard_D2s_v3"
kubernetes_version = "1.28.0"

# Tags
tags = {
  Environment = "dev"
  Project     = "AKS Starter Kit"
  ManagedBy   = "Terraform"
}
```

Commit and push:

```bash
git add infra/envs/dev/terraform.tfvars
git commit -m "Configure dev environment variables"
git push origin main
```

### Phase 2: Deploy Infrastructure

#### Option 1: Via GitHub UI

1. Go to **Actions** tab
2. Click **Infrastructure - Deploy with Terraform**
3. Click **Run workflow**
4. Select:
   - Branch: `main`
   - Environment: `dev`
   - Action: `apply`
5. Click **Run workflow**
6. Wait for approval (if configured)
7. Monitor the workflow execution

#### Option 2: Via Git Push

The infrastructure workflow will automatically run when you push changes to `infra/**` on the `main` branch.

```bash
# Make changes to infrastructure
git add infra/
git commit -m "Update infrastructure configuration"
git push origin main
```

**Expected Duration:** 20-30 minutes

### Phase 3: Build Container Images

#### Trigger Frontend Build

```bash
# Make a change to frontend or trigger manually
cd app/frontend
# Make a small change
echo "# Build trigger" >> README.md
git add .
git commit -m "Trigger frontend build"
git push origin main
```

Or trigger manually:
1. Go to **Actions** → **Frontend - Build & Test**
2. Click **Run workflow**
3. Click **Run workflow**

#### Trigger Backend Build

```bash
# Make a change to backend or trigger manually
cd app/backend
echo "# Build trigger" >> README.md
git add .
git commit -m "Trigger backend build"
git push origin main
```

Or trigger manually:
1. Go to **Actions** → **Backend - Build & Test**
2. Click **Run workflow**
3. Click **Run workflow**

**Expected Duration:** 5-10 minutes per workflow

### Phase 4: Deploy Applications

1. Go to **Actions** → **Application - Deploy to AKS**
2. Click **Run workflow**
3. Select:
   - Environment: `dev`
   - Frontend tag: `latest` (or specific build number)
   - Backend tag: `latest` (or specific build number)
4. Click **Run workflow**
5. Monitor deployment

**Expected Duration:** 3-5 minutes

### Phase 5: Verify Deployment

```bash
# Login to Azure
az login

# Set subscription
az account set --subscription "<YOUR_SUBSCRIPTION_ID>"

# Get AKS credentials
az aks get-credentials \
  --resource-group rg-aksstarter-dev \
  --name aks-aksstarter-dev

# Check deployments
kubectl get deployments -n dev
kubectl get pods -n dev
kubectl get services -n dev
kubectl get ingress -n dev

# Check pod logs
kubectl logs -n dev deployment/backend --tail=50
kubectl logs -n dev deployment/frontend --tail=50
```

---

## Workflow Overview

### 1. Frontend Build Workflow (`.github/workflows/build-frontend.yml`)

**Triggers:**
- Push to `main` or `develop` branches (when frontend files change)
- Pull requests to `main` or `develop`
- Manual trigger

**Steps:**
1. Checkout code
2. Setup Node.js 18
3. Install dependencies
4. Run linter
5. Run unit tests with coverage
6. Build production bundle
7. Build Docker image
8. Run Trivy security scan
9. Push image to ACR

**Artifacts:**
- Frontend build files
- Code coverage report
- Security scan results

### 2. Backend Build Workflow (`.github/workflows/build-backend.yml`)

**Triggers:**
- Push to `main` or `develop` branches (when backend files change)
- Pull requests to `main` or `develop`
- Manual trigger

**Steps:**
1. Checkout code
2. Setup JDK 17
3. Run Checkstyle
4. Check code formatting (Spotless)
5. Run unit tests
6. Run integration tests (BDD with Cucumber)
7. Generate code coverage
8. Build JAR
9. Build Docker image
10. Run Trivy security scan
11. Push image to ACR
12. (Optional) SonarQube analysis

**Artifacts:**
- Backend JAR file
- Test results (JUnit + Cucumber)
- Code coverage report
- Cucumber HTML reports
- Security scan results

### 3. Infrastructure Deployment Workflow (`.github/workflows/deploy-infrastructure.yml`)

**Triggers:**
- Push to `main` (when infra files change)
- Pull requests to `main`
- Manual trigger

**Steps:**
1. Checkout code
2. Setup Terraform
3. Azure login
4. Terraform format check
5. Terraform init
6. Terraform validate
7. Run tfsec security scan
8. Terraform plan
9. (Wait for approval)
10. Terraform apply
11. Output resource details

**Environments:**
- `dev`, `stage`, `prod`

### 4. Application Deployment Workflow (`.github/workflows/deploy-app.yml`)

**Triggers:**
- Successful completion of build workflows
- Manual trigger

**Steps:**
1. Checkout code
2. Azure login
3. Get AKS credentials
4. Setup Helm
5. Create namespace
6. Create image pull secrets
7. Create Azure AD secrets
8. Deploy backend with Helm
9. Deploy frontend with Helm
10. Wait for rollout
11. Run smoke tests
12. Publish deployment status

**Environments:**
- `dev` → `stage` → `prod` (sequential with approvals)

---

## Workflow Execution Flow

```
┌─────────────────────────────────────────────────────────────┐
│  Developer pushes code to main branch                       │
└──────────────────┬──────────────────────────────────────────┘
                   │
                   ├──────────────────┬──────────────────────────┐
                   │                  │                          │
                   ▼                  ▼                          ▼
        ┌──────────────────┐  ┌─────────────────┐  ┌──────────────────┐
        │ Infrastructure   │  │ Frontend Build  │  │ Backend Build    │
        │ Deployment       │  │ & Test          │  │ & Test           │
        │                  │  │                 │  │                  │
        │ • Terraform plan │  │ • Lint          │  │ • Checkstyle     │
        │ • Security scan  │  │ • Unit tests    │  │ • Spotless       │
        │ • Apply (manual) │  │ • Build         │  │ • Unit tests     │
        │ • Output         │  │ • Docker build  │  │ • BDD tests      │
        │                  │  │ • Trivy scan    │  │ • Docker build   │
        │                  │  │ • Push to ACR   │  │ • Trivy scan     │
        │                  │  │                 │  │ • Push to ACR    │
        └──────────────────┘  └────────┬────────┘  └────────┬─────────┘
                                       │                    │
                                       └──────────┬─────────┘
                                                  │
                                                  ▼
                                    ┌─────────────────────────┐
                                    │ Application Deployment  │
                                    │                         │
                                    │ • Deploy to dev         │
                                    │ • Smoke tests           │
                                    │                         │
                                    │ • Approval required     │
                                    │ • Deploy to stage       │
                                    │ • Integration tests     │
                                    │                         │
                                    │ • Approval required     │
                                    │ • Deploy to prod        │
                                    │ • Production tests      │
                                    │ • Monitor               │
                                    └─────────────────────────┘
```

---

## Troubleshooting

### Issue 1: Workflow Fails at Azure Login

**Symptom:**
```
Error: Error: Login failed with Error: getaddrinfo ENOTFOUND management.azure.com
```

**Solution:**
1. Verify `AZURE_CREDENTIALS` secret is correctly formatted JSON
2. Verify service principal hasn't expired
3. Check that all fields are present in the JSON

### Issue 2: Terraform State Lock

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

### Issue 3: Docker Build Fails

**Symptom:**
```
Error: buildx call failed: ...
```

**Solution:**
1. Check Dockerfile syntax
2. Verify all build arguments are provided
3. Check Docker build logs in workflow

### Issue 4: ACR Authentication Failed

**Symptom:**
```
Error: unauthorized: authentication required
```

**Solution:**
1. Verify `ACR_LOGIN_SERVER`, `ACR_USERNAME`, `ACR_PASSWORD` secrets
2. Check that ACR admin user is enabled:
   ```bash
   az acr update --name <ACR_NAME> --admin-enabled true
   ```

### Issue 5: Helm Deployment Fails

**Symptom:**
```
Error: UPGRADE FAILED: timed out waiting for the condition
```

**Solution:**
1. Check pod events: `kubectl describe pod -n dev <POD_NAME>`
2. Check pod logs: `kubectl logs -n dev <POD_NAME>`
3. Verify image exists in ACR
4. Check resource limits aren't too low

### Issue 6: Required Approvers Not Notified

**Symptom:**
Workflow is waiting for approval but approvers don't receive notification

**Solution:**
1. Check GitHub notification settings
2. Verify email is verified in GitHub
3. Check spam/junk folder
4. Approvers can manually check **Actions** tab for pending workflows

### Issue 7: Secrets Not Available in Workflow

**Symptom:**
```
Error: Secret AZURE_CREDENTIALS is not defined
```

**Solution:**
1. Verify secret name matches exactly (case-sensitive)
2. Check secret is defined at repository level (not environment level, unless needed)
3. Re-create secret if needed

---

## Best Practices

### 1. Branch Protection

Set up branch protection for `main`:

1. Go to **Settings** → **Branches**
2. Click **Add rule**
3. Branch name pattern: `main`
4. Enable:
   - ✓ Require a pull request before merging
   - ✓ Require status checks to pass before merging
   - ✓ Require branches to be up to date before merging
   - ✓ Include administrators
5. Save changes

### 2. Required Status Checks

Add these required status checks:
- `Code Quality Checks`
- `Build and Test Frontend` or `Build and Test Backend`

### 3. Security

- ✓ Never commit secrets to Git
- ✓ Rotate service principal credentials regularly (every 90 days)
- ✓ Use least-privilege access for service principals
- ✓ Enable Dependabot for dependency updates
- ✓ Enable code scanning (CodeQL)
- ✓ Review Trivy security scan results

### 4. Cost Management

- ✓ Destroy dev/stage environments when not needed
- ✓ Use workflow dispatch for manual control
- ✓ Set up Azure cost alerts
- ✓ Monitor GitHub Actions minutes usage

---

## Next Steps

After successful setup:

1. **Configure custom domain** - Update DNS records to point to Application Gateway
2. **Set up monitoring** - Configure Azure Monitor alerts
3. **Enable autoscaling** - Test HPA configuration
4. **Document runbooks** - Create operational procedures
5. **Train team** - Ensure team understands workflows
6. **Test disaster recovery** - Verify backup and restore procedures

---

## Summary

### Checklist

**Azure Setup:**
- [ ] Azure AD app registrations created (frontend + backend)
- [ ] Service principal created for GitHub Actions
- [ ] Terraform state storage created
- [ ] ACR created
- [ ] All credentials recorded securely

**GitHub Setup:**
- [ ] Repository created and code pushed
- [ ] All secrets configured (16 secrets)
- [ ] All variables configured (8 variables)
- [ ] Environments created (dev, stage, prod, prod-destroy)
- [ ] Branch protection enabled

**First Deployment:**
- [ ] Terraform variables updated
- [ ] Infrastructure deployed successfully
- [ ] Frontend build successful
- [ ] Backend build successful
- [ ] Applications deployed to dev
- [ ] Smoke tests passing

**Security:**
- [ ] No secrets in code
- [ ] Trivy scans configured
- [ ] tfsec scans configured
- [ ] Branch protection enabled
- [ ] Required approvals configured

---

For more details, see:
- [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) - Comprehensive deployment guide
- [ARCHITECTURE_DETAILED.md](ARCHITECTURE_DETAILED.md) - Architecture overview
- [QUICKSTART.md](docs/QUICKSTART.md) - Quick start guide
