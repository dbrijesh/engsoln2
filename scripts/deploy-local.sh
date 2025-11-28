#!/bin/bash
# Script to deploy applications locally using Minikube or Kind

set -e

echo "🚀 AKS Starter Kit - Local Deployment"
echo "======================================"

# Check prerequisites
command -v kubectl >/dev/null 2>&1 || { echo "❌ kubectl is required but not installed. Aborting." >&2; exit 1; }
command -v helm >/dev/null 2>&1 || { echo "❌ Helm is required but not installed. Aborting." >&2; exit 1; }
command -v docker >/dev/null 2>&1 || { echo "❌ Docker is required but not installed. Aborting." >&2; exit 1; }

# Check for local Kubernetes cluster
if ! kubectl cluster-info &>/dev/null; then
    echo "❌ No Kubernetes cluster found. Please start Minikube or Kind."
    echo ""
    echo "To start Minikube:"
    echo "  minikube start --cpus=4 --memory=8192"
    echo ""
    echo "To start Kind:"
    echo "  kind create cluster --name aks-starter"
    exit 1
fi

echo "✅ Kubernetes cluster detected"

# Build Docker images
echo ""
echo "📦 Building Docker images..."
echo "----------------------------"

echo "Building frontend..."
cd app/frontend
docker build -t aks-starter-frontend:local .
cd ../..

echo "Building backend..."
cd app/backend
docker build -t aks-starter-backend:local .
cd ../..

# If using Minikube, load images
if command -v minikube &> /dev/null && minikube status &>/dev/null; then
    echo ""
    echo "📥 Loading images into Minikube..."
    minikube image load aks-starter-frontend:local
    minikube image load aks-starter-backend:local
fi

# If using Kind, load images
if command -v kind &> /dev/null; then
    CLUSTER_NAME=$(kubectl config current-context | grep -o "kind-.*" | sed 's/kind-//' || echo "")
    if [ -n "$CLUSTER_NAME" ]; then
        echo ""
        echo "📥 Loading images into Kind..."
        kind load docker-image aks-starter-frontend:local --name "$CLUSTER_NAME"
        kind load docker-image aks-starter-backend:local --name "$CLUSTER_NAME"
    fi
fi

# Create namespace
echo ""
echo "📂 Creating namespace..."
kubectl create namespace dev --dry-run=client -o yaml | kubectl apply -f -

# Create secrets (mock values for local)
echo ""
echo "🔐 Creating secrets..."
kubectl create secret generic backend-secrets \
  --from-literal=tenant-id=local-tenant-id \
  --from-literal=client-id=local-client-id \
  --namespace=dev \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl create secret generic frontend-secrets \
  --from-literal=tenant-id=local-tenant-id \
  --from-literal=client-id=local-client-id \
  --namespace=dev \
  --dry-run=client -o yaml | kubectl apply -f -

# Deploy with Helm
echo ""
echo "⎈ Deploying with Helm..."
echo "------------------------"

echo "Deploying backend..."
helm upgrade --install backend ./charts/backend \
  --namespace dev \
  --set image.repository=aks-starter-backend \
  --set image.tag=local \
  --set image.pullPolicy=Never \
  --set ingress.enabled=false \
  --set service.type=NodePort

echo "Deploying frontend..."
helm upgrade --install frontend ./charts/frontend \
  --namespace dev \
  --set image.repository=aks-starter-frontend \
  --set image.tag=local \
  --set image.pullPolicy=Never \
  --set ingress.enabled=false \
  --set service.type=NodePort

# Wait for deployments
echo ""
echo "⏳ Waiting for deployments to be ready..."
kubectl wait --for=condition=available --timeout=300s deployment/backend -n dev
kubectl wait --for=condition=available --timeout=300s deployment/frontend -n dev

echo ""
echo "✅ Deployment complete!"
echo ""
echo "📋 Application Status:"
echo "---------------------"
kubectl get all -n dev

echo ""
echo "🌐 Access your applications:"
echo "---------------------------"

if command -v minikube &> /dev/null && minikube status &>/dev/null; then
    echo "Backend:  $(minikube service backend -n dev --url)"
    echo "Frontend: $(minikube service frontend -n dev --url)"
    echo ""
    echo "Or use: minikube service list"
else
    BACKEND_PORT=$(kubectl get svc backend -n dev -o jsonpath='{.spec.ports[0].nodePort}')
    FRONTEND_PORT=$(kubectl get svc frontend -n dev -o jsonpath='{.spec.ports[0].nodePort}')
    echo "Backend:  http://localhost:$BACKEND_PORT"
    echo "Frontend: http://localhost:$FRONTEND_PORT"
    echo ""
    echo "Note: You may need to port-forward:"
    echo "  kubectl port-forward svc/backend 8080:8080 -n dev"
    echo "  kubectl port-forward svc/frontend 3000:80 -n dev"
fi

echo ""
echo "🎉 Local deployment successful!"
