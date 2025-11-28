# Quick Start Guide

This guide will help you get the AKS Starter Kit up and running in your Azure environment.

## Prerequisites

Before you begin, ensure you have the following installed and configured:

### Required Tools
- **Azure CLI** (v2.50+): `az --version`
- **Terraform** (v1.5+): `terraform --version`
- **Helm** (v3.12+): `helm version`
- **kubectl** (v1.27+): `kubectl version`
- **Node.js** (v18+): `node --version`
- **Java** (JDK 17+): `java --version`
- **Maven** (3.9+): `mvn --version`
- **Docker**: `docker --version`

### Azure Requirements
- Active Azure subscription
- Contributor or Owner role on the subscription
- Azure DevOps organization and project

### Azure Entra (Azure AD) Setup
You'll need to register applications in Azure AD:

1. **Backend API Application**:
   ```bash
   az ad app create --display-name "AKS Starter Backend API" \
     --sign-in-audience AzureADMyOrg \
     --enable-id-token-issuance true
   ```
   - Note the **Application (client) ID**
   - Note the **Directory (tenant) ID**
   - Add an API scope (e.g., `api://YOUR_API_CLIENT_ID/access_as_user`)

2. **Frontend SPA Application**:
   ```bash
   az ad app create --display-name "AKS Starter Frontend" \
     --sign-in-audience AzureADMyOrg \
     --web-redirect-uris "http://localhost:3000" \
     --enable-id-token-issuance true
   ```
   - Note the **Application (client) ID**
   - Configure API permissions to access the backend API

## Step 1: Local Development Setup

### Build and Test Frontend

```bash
cd app/frontend

# Install dependencies
npm install

# Configure environment variables
cp .env.example .env
# Edit .env and add your Azure AD client IDs

# Run tests
npm test

# Start development server
npm start
# Access at http://localhost:3000
```

### Build and Test Backend

```bash
cd app/backend

# Run tests
./mvnw clean test

# Run the application
./mvnw spring-boot:run
# Access at http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
```

## Step 2: Provision Azure Infrastructure

### Create Terraform Backend Storage

```bash
# Set variables
RESOURCE_GROUP="terraform-state-rg"
STORAGE_ACCOUNT="tfstateaksstarter"  # Must be globally unique
CONTAINER="tfstate"
LOCATION="eastus"

# Create resource group
az group create --name $RESOURCE_GROUP --location $LOCATION

# Create storage account
az storage account create \
  --resource-group $RESOURCE_GROUP \
  --name $STORAGE_ACCOUNT \
  --sku Standard_LRS \
  --encryption-services blob

# Create blob container
az storage container create \
  --name $CONTAINER \
  --account-name $STORAGE_ACCOUNT
```

### Deploy Infrastructure (Dev Environment)

```bash
cd infra/envs/dev

# Copy and customize variables
cp ../../terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars with your values

# Initialize Terraform
terraform init -backend-config="backend-dev.hcl"

# Review the plan
terraform plan

# Apply (this will take 15-30 minutes)
terraform apply

# Save outputs
terraform output -json > outputs.json
```

### Get AKS Credentials

```bash
# Get cluster name and resource group from Terraform outputs
AKS_CLUSTER=$(terraform output -raw aks_cluster_name)
RESOURCE_GROUP=$(terraform output -raw resource_group_name)

# Get credentials
az aks get-credentials \
  --resource-group $RESOURCE_GROUP \
  --name $AKS_CLUSTER \
  --overwrite-existing

# Verify connection
kubectl get nodes
```

## Step 3: Build and Push Container Images

### Login to Azure Container Registry

```bash
ACR_NAME=$(terraform output -raw acr_name)

az acr login --name $ACR_NAME
```

### Build and Push Frontend

```bash
cd app/frontend

docker build -t $ACR_NAME.azurecr.io/frontend:v1.0.0 .
docker push $ACR_NAME.azurecr.io/frontend:v1.0.0
docker tag $ACR_NAME.azurecr.io/frontend:v1.0.0 $ACR_NAME.azurecr.io/frontend:latest
docker push $ACR_NAME.azurecr.io/frontend:latest
```

### Build and Push Backend

```bash
cd app/backend

docker build -t $ACR_NAME.azurecr.io/backend:v1.0.0 .
docker push $ACR_NAME.azurecr.io/backend:v1.0.0
docker tag $ACR_NAME.azurecr.io/backend:v1.0.0 $ACR_NAME.azurecr.io/backend:latest
docker push $ACR_NAME.azurecr.io/backend:latest
```

## Step 4: Deploy Applications to AKS

### Create Kubernetes Secrets

```bash
# Get Key Vault name
KV_NAME=$(terraform output -raw keyvault_name)

# Store secrets in Key Vault
az keyvault secret set --vault-name $KV_NAME --name "backend-tenant-id" --value "YOUR_TENANT_ID"
az keyvault secret set --vault-name $KV_NAME --name "backend-client-id" --value "YOUR_BACKEND_CLIENT_ID"
az keyvault secret set --vault-name $KV_NAME --name "frontend-client-id" --value "YOUR_FRONTEND_CLIENT_ID"
az keyvault secret set --vault-name $KV_NAME --name "frontend-tenant-id" --value "YOUR_TENANT_ID"

# Create Kubernetes secrets from Key Vault
kubectl create secret generic backend-secrets \
  --from-literal=tenant-id="YOUR_TENANT_ID" \
  --from-literal=client-id="YOUR_BACKEND_CLIENT_ID" \
  -n dev

kubectl create secret generic frontend-secrets \
  --from-literal=tenant-id="YOUR_TENANT_ID" \
  --from-literal=client-id="YOUR_FRONTEND_CLIENT_ID" \
  -n dev
```

### Deploy with Helm

```bash
# Create namespace
kubectl create namespace dev

# Update Helm chart values
# Edit charts/backend/values.yaml and charts/frontend/values.yaml
# Update image repository to match your ACR

# Deploy backend
helm upgrade --install backend ./charts/backend \
  --namespace dev \
  --set image.repository=$ACR_NAME.azurecr.io/backend \
  --set image.tag=v1.0.0

# Deploy frontend
helm upgrade --install frontend ./charts/frontend \
  --namespace dev \
  --set image.repository=$ACR_NAME.azurecr.io/frontend \
  --set image.tag=v1.0.0

# Check deployment status
kubectl get all -n dev
```

### Verify Deployment

```bash
# Check pods
kubectl get pods -n dev

# Check services
kubectl get svc -n dev

# Check ingress (if configured)
kubectl get ingress -n dev

# View logs
kubectl logs -f deployment/backend -n dev
kubectl logs -f deployment/frontend -n dev

# Test health endpoints
kubectl port-forward svc/backend 8080:8080 -n dev
# In another terminal: curl http://localhost:8080/actuator/health
```

## Step 5: Setup Azure DevOps Pipelines

### Create Service Connections

1. **Azure Resource Manager Connection**:
   - Go to Project Settings > Service connections
   - Create new Azure Resource Manager connection
   - Name it `azure-service-connection`
   - Grant access to your subscription

2. **Azure Container Registry Connection**:
   - Create Docker Registry service connection
   - Name it `acr-service-connection`
   - Connect to your ACR

3. **SonarQube Connection** (optional):
   - Create SonarQube service connection
   - Name it `sonarqube-service-connection`

### Import Pipelines

1. **Frontend Build Pipeline**:
   ```bash
   # Go to Pipelines > New Pipeline > Azure Repos Git
   # Select: azure-pipelines/build-frontend.yml
   # Save and run
   ```

2. **Backend Build Pipeline**:
   ```bash
   # Select: azure-pipelines/build-backend.yml
   # Update variables if needed
   # Save and run
   ```

3. **Infrastructure Pipeline**:
   ```bash
   # Select: azure-pipelines/infra-deploy.yml
   # Configure parameters
   # Save (manual trigger)
   ```

4. **Release Pipeline**:
   ```bash
   # Select: azure-pipelines/release-deploy.yml
   # Configure environments: dev, stage, prod
   # Save (manual trigger)
   ```

### Configure Pipeline Variables

Add these variables to your pipelines (as secrets where appropriate):

- `AZURE_TENANT_ID`: Your Azure AD tenant ID
- `SONAR_TOKEN`: SonarQube authentication token
- Update ACR name and other environment-specific values

## Step 6: Local Development with Minikube/Kind

### Using Minikube

```bash
# Start Minikube
minikube start --cpus=4 --memory=8192

# Deploy locally
./scripts/deploy-local.sh

# Access services
minikube service list
```

### Using Kind

```bash
# Create cluster
kind create cluster --name aks-starter

# Deploy locally
./scripts/deploy-local.sh

# Port forward to access
kubectl port-forward svc/frontend 3000:80 -n dev
kubectl port-forward svc/backend 8080:8080 -n dev
```

## Troubleshooting

### Common Issues

**Terraform State Lock**:
```bash
# If Terraform state is locked
terraform force-unlock LOCK_ID
```

**AKS Connection Issues**:
```bash
# Reset credentials
az aks get-credentials --resource-group RESOURCE_GROUP --name AKS_CLUSTER --overwrite-existing

# Check cluster health
kubectl get nodes
kubectl get pods -A
```

**Pod Crashes**:
```bash
# Check pod logs
kubectl logs POD_NAME -n NAMESPACE

# Describe pod for events
kubectl describe pod POD_NAME -n NAMESPACE

# Check resource constraints
kubectl top pods -n NAMESPACE
```

**Image Pull Errors**:
```bash
# Verify ACR integration
az aks check-acr --resource-group RESOURCE_GROUP --name AKS_CLUSTER --acr ACR_NAME.azurecr.io
```

**Azure AD Authentication Issues**:
- Verify redirect URIs are configured correctly
- Check API permissions are granted
- Ensure token audience matches backend configuration

## Next Steps

- Review [architecture.md](architecture.md) for system design details
- Follow [CHECKLIST.md](CHECKLIST.md) to adapt for your application
- Configure monitoring and alerting
- Set up CI/CD automation
- Implement backup and disaster recovery

## Cleanup

To destroy all resources and avoid Azure costs:

```bash
# Destroy infrastructure
./scripts/cleanup-azure.sh dev

# Or manually
cd infra/envs/dev
terraform destroy
```

## Support

For issues or questions:
- Check logs: `kubectl logs` and `az aks browse`
- Review Azure Portal for resource status
- Consult Terraform/Kubernetes documentation
- Review pipeline logs in Azure DevOps
