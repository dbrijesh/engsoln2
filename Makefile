# Makefile for AKS Starter Kit
.PHONY: help install build test docker-build deploy-local clean quality-check format

# Default target
help:
	@echo "AKS Starter Kit - Available Commands:"
	@echo ""
	@echo "Local Development:"
	@echo "  make install         - Install all dependencies"
	@echo "  make build           - Build frontend and backend"
	@echo "  make test            - Run all tests"
	@echo "  make quality-check   - Run code quality checks (Checkstyle + Spotless)"
	@echo "  make format          - Auto-format code with Spotless"
	@echo "  make docker-build    - Build Docker images"
	@echo "  make docker-up       - Start with Docker Compose"
	@echo "  make docker-down     - Stop Docker Compose"
	@echo ""
	@echo "Kubernetes:"
	@echo "  make deploy-local    - Deploy to local Kubernetes"
	@echo "  make helm-install    - Install Helm charts"
	@echo "  make helm-uninstall  - Uninstall Helm charts"
	@echo ""
	@echo "Azure:"
	@echo "  make tf-init ENV=dev - Initialize Terraform"
	@echo "  make tf-plan ENV=dev - Plan Terraform changes"
	@echo "  make tf-apply ENV=dev- Apply Terraform changes"
	@echo ""
	@echo "Cleanup:"
	@echo "  make clean           - Clean build artifacts"
	@echo "  make destroy ENV=dev - Destroy Azure resources"

# Install dependencies
install:
	@echo "Installing frontend dependencies..."
	cd app/frontend && npm install
	@echo "Installing backend dependencies..."
	cd app/backend && ./mvnw dependency:resolve
	@echo "Done!"

# Build all
build:
	@echo "Building frontend..."
	cd app/frontend && npm run build
	@echo "Building backend..."
	cd app/backend && ./mvnw clean package -DskipTests
	@echo "Build complete!"

# Run tests
test:
	@echo "Running frontend tests..."
	cd app/frontend && npm test
	@echo "Running backend tests..."
	cd app/backend && ./mvnw test
	@echo "Tests complete!"

# Code quality checks
quality-check:
	@echo "Running code quality checks..."
	cd app/backend && ./mvnw checkstyle:check spotless:check
	@echo "Quality checks passed!"

# Auto-format code
format:
	@echo "Formatting backend code..."
	cd app/backend && ./mvnw spotless:apply
	@echo "Code formatted!"

# Build Docker images
docker-build:
	@echo "Building frontend image..."
	docker build -t aks-starter-frontend:local ./app/frontend
	@echo "Building backend image..."
	docker build -t aks-starter-backend:local ./app/backend
	@echo "Docker images built!"

# Start with Docker Compose
docker-up:
	docker-compose up -d
	@echo "Services started! Frontend: http://localhost:3000, Backend: http://localhost:8080"

# Stop Docker Compose
docker-down:
	docker-compose down

# Deploy to local Kubernetes
deploy-local:
	@echo "Deploying to local Kubernetes..."
	./scripts/deploy-local.sh

# Install Helm charts
helm-install:
	@echo "Installing Helm charts..."
	helm upgrade --install backend ./charts/backend -n dev --create-namespace
	helm upgrade --install frontend ./charts/frontend -n dev
	@echo "Helm charts installed!"

# Uninstall Helm charts
helm-uninstall:
	helm uninstall backend -n dev || true
	helm uninstall frontend -n dev || true

# Terraform init
tf-init:
	@if [ -z "$(ENV)" ]; then echo "Usage: make tf-init ENV=dev"; exit 1; fi
	cd infra/envs/$(ENV) && terraform init -backend-config="backend-$(ENV).hcl"

# Terraform plan
tf-plan:
	@if [ -z "$(ENV)" ]; then echo "Usage: make tf-plan ENV=dev"; exit 1; fi
	cd infra/envs/$(ENV) && terraform plan

# Terraform apply
tf-apply:
	@if [ -z "$(ENV)" ]; then echo "Usage: make tf-apply ENV=dev"; exit 1; fi
	cd infra/envs/$(ENV) && terraform apply

# Clean build artifacts
clean:
	@echo "Cleaning build artifacts..."
	rm -rf app/frontend/build
	rm -rf app/frontend/node_modules
	rm -rf app/backend/target
	@echo "Clean complete!"

# Destroy Azure resources
destroy:
	@if [ -z "$(ENV)" ]; then echo "Usage: make destroy ENV=dev"; exit 1; fi
	./scripts/cleanup-azure.sh $(ENV)
