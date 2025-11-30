# GitHub Actions Configuration Guide

Complete guide for configuring GitHub Actions secrets and variables for the AKS Starter Kit CI/CD pipelines.

## Table of Contents

- [Overview](#overview)
- [Repository Secrets](#repository-secrets)
- [Environment Configuration](#environment-configuration)
- [How to Configure](#how-to-configure)
- [Workflows Overview](#workflows-overview)
- [Verification](#verification)
- [Troubleshooting](#troubleshooting)

## Overview

This repository uses GitHub Actions for CI/CD automation:
- Building and testing frontend (React) and backend (Spring Boot)
- Deploying to Azure Kubernetes Service (AKS)
- Managing Azure infrastructure with Terraform

## Repository Secrets

Configure these secrets at **Settings > Secrets and variables > Actions > Secrets**.

### Azure Authentication

**`AZURE_CREDENTIALS`** - Service principal credentials (JSON format)

Create with:
```bash
az ad sp create-for-rbac --name "github-actions-aksstarter" \
  --role contributor \
  --scopes /subscriptions/{subscription-id} \
  --sdk-auth
```

### Azure Container Registry

**`ACR_LOGIN_SERVER`** - ACR URL (e.g., `acraksstarter29112025.azurecr.io`)
**`ACR_USERNAME`** - ACR admin username
**`ACR_PASSWORD`** - ACR admin password

Get credentials:
```bash
az acr credential show --name {acr-name}
```

### Azure AD Authentication

**`REACT_APP_CLIENT_ID`** - Frontend SPA application ID
**`BACKEND_CLIENT_ID`** - Backend API application ID (without api:// prefix)
**`ARM_TENANT_ID`** - Azure AD tenant ID

## Environment Configuration

Create three environments: dev, stage, prod at **Settings > Environments**.

### Environment Variables

**Dev Environment:**
- `AKS_RESOURCE_GROUP_DEV` - Resource group name
- `AKS_CLUSTER_NAME_DEV` - AKS cluster name
- Protection: None (auto-deploy)

**Stage Environment:**
- `AKS_RESOURCE_GROUP_STAGE` - Resource group name
- `AKS_CLUSTER_NAME_STAGE` - AKS cluster name
- Protection: 1 required reviewer

**Prod Environment:**
- `AKS_RESOURCE_GROUP_PROD` - Resource group name
- `AKS_CLUSTER_NAME_PROD` - AKS cluster name
- Protection: 2 required reviewers, main branch only

## Workflows Overview

### Build Workflows

**build-frontend.yml** - Builds frontend Docker image
- Triggers: Push to main/develop with app/frontend changes
- Outputs: Docker image tagged with commit SHA and latest

**build-backend.yml** - Builds backend Docker image
- Triggers: Push to main/develop with app/backend changes
- Outputs: Docker image tagged with commit SHA and latest

### Deployment Workflow

**deploy-app.yml** - Deploys to AKS
- Triggers: After successful builds or manual dispatch
- Environments: dev, stage, prod
- Creates Kubernetes secrets with proper configuration

Key deployment steps:
1. Installs NGINX Ingress and cert-manager
2. Creates azure-ad-credentials secret (with api:// prefix for backend)
3. Deploys backend and frontend
4. Runs smoke tests

## Quick Setup

```bash
# Configure repository secrets
gh secret set AZURE_CREDENTIALS < azure-credentials.json
gh secret set ACR_LOGIN_SERVER --body "acraksstarter29112025.azurecr.io"
gh secret set ACR_USERNAME --body "$(az acr credential show --name acraksstarter29112025 --query username -o tsv)"
gh secret set ACR_PASSWORD --body "$(az acr credential show --name acraksstarter29112025 --query passwords[0].value -o tsv)"
gh secret set REACT_APP_CLIENT_ID --body "34b84e4c-1b3d-4325-bc4b-47e8ac4b7d18"
gh secret set BACKEND_CLIENT_ID --body "1e006404-4974-4df7-ba31-2266697efb69"
gh secret set ARM_TENANT_ID --body "$(az account show --query tenantId -o tsv)"
```

## Verification

Check secrets:
```bash
gh secret list
```

Test deployment:
```bash
gh workflow run deploy-app.yml -f environment=dev -f backend_tag=latest -f frontend_tag=latest
```

## Troubleshooting

### Backend 401 Unauthorized
- Verify BACKEND_CLIENT_ID is correct (without api:// prefix)
- Check ARM_TENANT_ID matches your tenant
- See AZURE_AD_CONFIGURATION.md for details

### ACR Authentication Failed
- Regenerate credentials: `az acr credential renew --name {acr-name}`
- Verify ACR_LOGIN_SERVER matches actual ACR name

### AKS Cluster Not Found
- Verify environment variables AKS_RESOURCE_GROUP and AKS_CLUSTER_NAME
- Ensure cluster exists: `az aks list -o table`

## Security Best Practices

1. Rotate credentials every 90 days
2. Use service principals with least privilege
3. Enable GitHub Advanced Security for secret scanning
4. Use different service principals per environment
5. Review workflow logs regularly

## Complete Secrets Checklist

Repository Secrets:
- [ ] AZURE_CREDENTIALS
- [ ] ACR_LOGIN_SERVER
- [ ] ACR_USERNAME
- [ ] ACR_PASSWORD
- [ ] REACT_APP_CLIENT_ID
- [ ] BACKEND_CLIENT_ID
- [ ] ARM_TENANT_ID

Environment Variables (per environment):
- [ ] AKS_RESOURCE_GROUP_{ENV}
- [ ] AKS_CLUSTER_NAME_{ENV}

Environment Protection:
- [ ] Dev: No restrictions
- [ ] Stage: 1 reviewer
- [ ] Prod: 2 reviewers, main only

## Additional Resources

- Azure AD setup: AZURE_AD_CONFIGURATION.md
- Development guide: CLAUDE.md
- GitHub Actions docs: https://docs.github.com/en/actions
- Azure service principals: https://learn.microsoft.com/azure/developer/github/connect-from-azure

---

**Last Updated**: 2025-11-30
**Maintained By**: DevOps Team
