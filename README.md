# Azure AKS Enterprise Starter Kit

A production-ready, fully reusable engineering starter solution for deploying applications to Azure Kubernetes Service (AKS) with complete CI/CD automation.

## 🚀 Quick Start

```bash
# 1. Build and test locally
cd app/frontend && npm install && npm test
cd ../backend && ./mvnw clean test

# 2. Provision Azure infrastructure (requires Azure CLI + credentials)
cd infra/envs/dev
terraform init
terraform plan
terraform apply

# 3. Deploy applications to AKS
cd ../../../charts
helm upgrade --install frontend ./frontend -n dev --create-namespace
helm upgrade --install backend ./backend -n dev

# 4. Import Azure DevOps pipelines
# See docs/QUICKSTART.md for detailed pipeline setup
```

## 📦 What's Included

### Applications
- **Frontend**: React 18+ SPA with Azure Entra SSO (MSAL.js), Jest tests, Cypress E2E
- **Backend**: Spring Boot 3.x REST API with security, OpenAPI, Resilience4j, Cucumber BDD tests

### Infrastructure as Code
- **Terraform modules** for AKS, ACR, Key Vault, Application Gateway WAF, API Management, monitoring
- **Environment configs** for dev, stage, and prod with variable overrides

### Kubernetes Deployment
- **Helm charts** with best practices: probes, HPA, PDB, secrets management via CSI driver
- **TLS/HTTPS** ingress configuration with Application Gateway

### CI/CD Pipelines (Azure DevOps)
- **Build pipelines**: compile, test, coverage, container build, security scan, push to ACR
- **Infrastructure pipeline**: Terraform validate, plan, apply with approval gates
- **Release pipeline**: Helm deploy, integration tests, DAST scanning

### Security & Quality
- **Static analysis**: SonarQube/SonarCloud for code quality
- **IaC scanning**: tfsec + tflint for Terraform
- **Container scanning**: Trivy for image vulnerabilities
- **DAST**: OWASP ZAP for runtime security testing
- **Secrets management**: Azure Key Vault with CSI driver integration

### Testing
- **Unit tests**: Jest (frontend), JUnit 5 (backend) with coverage thresholds
- **BDD integration tests**: Cucumber with Gherkin feature files
- **E2E tests**: Cypress for frontend workflows

### Observability
- **Health/metrics endpoints** for Kubernetes probes
- **Structured logging** with correlation IDs
- **Azure Monitor** integration with Log Analytics
- **Prometheus/Grafana** configurations

## 📁 Repository Structure

```
.
├── README.md                          # This file
├── app/
│   ├── frontend/                      # React 18 SPA
│   │   ├── src/                       # Source code
│   │   ├── public/                    # Static assets
│   │   ├── tests/                     # Jest + Cypress tests
│   │   ├── package.json
│   │   ├── Dockerfile
│   │   └── nginx.conf
│   └── backend/                       # Spring Boot 3.x API
│       ├── src/main/java/             # Application code
│       ├── src/test/java/             # Unit tests
│       ├── src/test/resources/        # Cucumber features
│       ├── pom.xml
│       └── Dockerfile
├── infra/
│   ├── modules/                       # Reusable Terraform modules
│   │   ├── aks/
│   │   ├── acr/
│   │   ├── keyvault/
│   │   ├── appgw_waf/
│   │   ├── apim/
│   │   └── monitoring/
│   ├── envs/                          # Environment-specific configs
│   │   ├── dev/
│   │   ├── stage/
│   │   └── prod/
│   ├── main.tf
│   ├── variables.tf
│   ├── outputs.tf
│   └── terraform.tfvars.example
├── charts/                            # Helm charts
│   ├── frontend/
│   └── backend/
├── azure-pipelines/                   # Azure DevOps YAML pipelines
│   ├── build-frontend.yml
│   ├── build-backend.yml
│   ├── infra-deploy.yml
│   └── release-deploy.yml
├── ci-scripts/                        # Helper scripts for pipelines
│   ├── run-sonar-scan.sh
│   ├── run-trivy-scan.sh
│   └── run-zap-scan.sh
├── docs/
│   ├── QUICKSTART.md                  # Detailed setup guide
│   ├── CHECKLIST.md                   # Customization guide
│   └── architecture.md                # Architecture diagrams
└── scripts/
    ├── deploy-local.sh                # Local development deploy
    └── cleanup-azure.sh               # Destroy Azure resources
```

## 🎯 Key Features

### Security First
- HTTPS everywhere with TLS termination
- WAF v2 with OWASP top 10 protection
- Managed identities for Azure services
- Secrets stored in Key Vault, never in code
- RBAC and least privilege access
- Multi-layer security scanning in CI/CD

### Production Ready
- High availability with multi-node AKS cluster
- Auto-scaling (HPA + cluster autoscaler)
- Health/readiness probes
- Pod disruption budgets
- Resource requests/limits
- Resilience patterns (circuit breaker, retries, rate limiting)

### Fully Automated
- Infrastructure provisioning via Terraform
- Application deployment via Helm
- Complete CI/CD with Azure DevOps
- Automated testing at every stage
- Approval gates for production

### Highly Reusable
- Modular Terraform modules
- Templated Helm charts
- Parameterized pipelines
- Clear configuration separation
- Easy to adapt for new applications

## 🔧 Prerequisites

- **Azure Subscription** with Owner or Contributor role
- **Azure CLI** (v2.50+)
- **Terraform** (v1.5+)
- **Helm** (v3.12+)
- **kubectl** (v1.27+)
- **Node.js** (v18+) and npm
- **Java** (JDK 17+) and Maven
- **Docker** for local builds
- **Azure DevOps** organization and project

## 📚 Documentation

- [QUICKSTART.md](docs/QUICKSTART.md) - Step-by-step setup instructions
- [architecture.md](docs/architecture.md) - System architecture and diagrams
- [CHECKLIST.md](docs/CHECKLIST.md) - Guide to adapt for your application

## 💰 Cost Considerations

This solution provisions production-grade Azure resources. Expected monthly costs (dev environment):
- AKS cluster: ~$150-300 (2-3 nodes)
- Application Gateway WAF: ~$150-200
- API Management: ~$50-100 (Developer tier)
- ACR: ~$5-20
- Key Vault: ~$1-5
- Log Analytics: ~$20-50

**Total estimated: $350-650/month for dev environment**

To minimize costs:
- Use smaller VM SKUs for dev/stage
- Scale down AKS nodes when not in use
- Use lower-tier APIM SKU for non-prod
- Destroy environments when not needed: `./scripts/cleanup-azure.sh dev`

## 🧹 Cleanup

To destroy all Azure resources and avoid charges:

```bash
cd infra/envs/dev
terraform destroy

# Or use the helper script
./scripts/cleanup-azure.sh dev
```

## 🤝 Contributing

This is a starter template. To customize for your application:

1. Follow [CHECKLIST.md](docs/CHECKLIST.md) to replace Hello World with your app
2. Update variable files in `infra/envs/`
3. Modify Helm values in `charts/*/values.yaml`
4. Adjust pipeline variables in `azure-pipelines/*.yml`

## 📝 License

MIT License - feel free to use and modify for your projects.

## 🆘 Support

For issues or questions:
- Check [QUICKSTART.md](docs/QUICKSTART.md) for common setup problems
- Review Terraform/Helm documentation for configuration details
- Consult Azure DevOps docs for pipeline troubleshooting

---

**Built with ❤️ for production-grade Azure deployments**
