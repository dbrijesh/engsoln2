# Azure AD Configuration Guide

This document outlines the Azure AD app registration configuration for the AKS Starter Kit.

## Two-App Registration Setup (Production)

This application uses a **two-app registration** pattern:
1. **Frontend App** - Single-page application for user authentication
2. **Backend API App** - API application that validates JWT tokens

## App Registration Details

### Frontend App Registration
- **Application (client) ID**: `34b84e4c-1b3d-4325-bc4b-47e8ac4b7d18`
- **Type**: Single-page application (SPA)
- **Redirect URIs**: `https://172.212.47.35`, `https://172.212.47.35/`
- **Tenant ID**: `52451440-c2a9-442f-8c20-8562d49f6846`

### Backend API App Registration
- **Application (client) ID**: `1e006404-4974-4df7-ba31-2266697efb69`
- **Type**: Web API
- **Application ID URI**: `api://1e006404-4974-4df7-ba31-2266697efb69`
- **Exposed API Scope**: `api://1e006404-4974-4df7-ba31-2266697efb69/access_as_user`
- **Authorized Client**: `34b84e4c-1b3d-4325-bc4b-47e8ac4b7d18` (frontend app)

## Azure AD Configuration Steps

### Backend API App
1. **Expose an API**:
   - Application ID URI: `api://1e006404-4974-4df7-ba31-2266697efb69`
   - Scope name: `access_as_user`
   - Who can consent: Admins and users
   - State: Enabled

2. **Authorized client applications**:
   - Client ID: `34b84e4c-1b3d-4325-bc4b-47e8ac4b7d18` (frontend)
   - Authorized scopes: `access_as_user`

### Frontend SPA App
1. **Platform configuration**:
   - Type: Single-page application
   - Redirect URIs: `https://172.212.47.35/`

2. **Authentication**:
   - Implicit grant: Disabled (uses PKCE flow)

## GitHub Secrets Configuration

The following secrets should be configured in GitHub repository settings:

```
REACT_APP_CLIENT_ID=34b84e4c-1b3d-4325-bc4b-47e8ac4b7d18
BACKEND_CLIENT_ID=1e006404-4974-4df7-ba31-2266697efb69
ARM_TENANT_ID=52451440-c2a9-442f-8c20-8562d49f6846
```

## Kubernetes Secrets

### For dev namespace:

```bash
# Backend Azure AD credentials
# IMPORTANT: client-id MUST include the "api://" prefix to match JWT audience claim
kubectl create secret generic azure-ad-credentials \
  --from-literal=client-id=api://1e006404-4974-4df7-ba31-2266697efb69 \
  --from-literal=tenant-id=52451440-c2a9-442f-8c20-8562d49f6846 \
  -n dev \
  --dry-run=client -o yaml | kubectl apply -f -

# Frontend environment variables
kubectl create secret generic frontend-secrets \
  --from-literal=REACT_APP_CLIENT_ID=34b84e4c-1b3d-4325-bc4b-47e8ac4b7d18 \
  --from-literal=REACT_APP_TENANT_ID=52451440-c2a9-442f-8c20-8562d49f6846 \
  --from-literal=REACT_APP_API_URL=https://172.212.47.35/api \
  --from-literal=REACT_APP_API_SCOPE=api://1e006404-4974-4df7-ba31-2266697efb69/access_as_user \
  -n dev \
  --dry-run=client -o yaml | kubectl apply -f -
```

## Environment Variables

### Frontend Container
- `REACT_APP_CLIENT_ID`: Frontend SPA app ID
- `REACT_APP_TENANT_ID`: Azure AD tenant ID
- `REACT_APP_API_URL`: Backend API endpoint
- `REACT_APP_API_SCOPE`: Backend API scope for token acquisition

### Backend Container
- `AZURE_CLIENT_ID`: Backend API app ID (for JWT audience validation)
- `AZURE_TENANT_ID`: Azure AD tenant ID (for issuer validation)
- `SPRING_PROFILES_ACTIVE`: Spring profile (dev/stage/prod)

## Token Flow

1. User signs in via frontend using MSAL.js (PKCE flow)
2. Frontend acquires token with scope `api://1e006404-4974-4df7-ba31-2266697efb69/access_as_user`
3. Frontend sends API request with `Authorization: Bearer {token}` header
4. Backend validates JWT token:
   - Issuer: `https://sts.windows.net/52451440-c2a9-442f-8c20-8562d49f6846/` (Azure AD v1.0)
   - Audience: `api://1e006404-4974-4df7-ba31-2266697efb69`
5. Backend extracts roles from token and authorizes request

## Troubleshooting

### Error: AADSTS500011 - Resource principal not found
- Ensure the backend API app is created in Azure AD
- Verify the Application ID URI is set: `api://1e006404-4974-4df7-ba31-2266697efb69`
- Confirm the scope `access_as_user` is exposed and enabled

### Error: AADSTS9002326 - Cross-origin token redemption
- Ensure frontend app is registered as "Single-page application" (not Web)
- Verify redirect URI matches exactly: `https://172.212.47.35/`

### Backend JWT validation fails (401 Unauthorized)

**Common Issue: Token version and audience mismatch**

Azure AD can issue tokens from two different endpoints:
- **v1.0 endpoint**: Issuer is `https://sts.windows.net/{tenant-id}/`
- **v2.0 endpoint**: Issuer is `https://login.microsoftonline.com/{tenant-id}/v2.0`

**This application is configured for Azure AD v1.0 tokens.** If your Azure AD app is configured to issue v2.0 tokens, you'll get 401 errors.

**Critical Configuration Points:**
1. **Backend expects audience**: `api://1e006404-4974-4df7-ba31-2266697efb69`
   - The `AZURE_CLIENT_ID` environment variable MUST include the `api://` prefix
   - Without it, Spring Security JWT validation will fail with audience mismatch

2. **Backend expects issuer**: `https://sts.windows.net/52451440-c2a9-442f-8c20-8562d49f6846/`
   - The `application.yml` sets `issuer-uri: https://sts.windows.net/${AZURE_TENANT_ID}/`
   - JWT tokens must be issued from Azure AD v1.0 endpoint

**Verification Steps:**
1. Decode your JWT token at https://jwt.ms
2. Check the `iss` claim matches: `https://sts.windows.net/52451440-c2a9-442f-8c20-8562d49f6846/`
3. Check the `aud` claim matches: `api://1e006404-4974-4df7-ba31-2266697efb69`
4. Verify Kubernetes secret has correct format:
   ```bash
   kubectl get secret azure-ad-credentials -n dev -o jsonpath='{.data.client-id}' | base64 -d
   # Should output: api://1e006404-4974-4df7-ba31-2266697efb69
   ```

**Other Checks:**
- Check `AZURE_CLIENT_ID` in backend matches the backend app ID (with `api://` prefix)
- Verify `AZURE_TENANT_ID` matches your Azure AD tenant
- Check backend logs for specific JWT validation errors: `kubectl logs -n dev deployment/backend`
