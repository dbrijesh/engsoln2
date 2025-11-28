# Terraform configuration for dev environment
terraform {
  required_version = ">= 1.5.0"

  backend "azurerm" {
    # Configure via backend config file:
    # terraform init -backend-config="backend-dev.hcl"
  }
}

module "infrastructure" {
  source = "../../"

  project_name = "aksstarter"
  environment  = "dev"
  location     = "eastus"

  tags = {
    Project     = "AKS Starter Kit"
    Environment = "dev"
    ManagedBy   = "Terraform"
  }

  # ACR
  acr_sku = "Standard"

  # AKS
  kubernetes_version = "1.27"
  aks_default_node_pool = {
    name                = "default"
    vm_size             = "Standard_D2s_v3"
    node_count          = 2
    min_count           = 2
    max_count           = 5
    enable_auto_scaling = true
    os_disk_size_gb     = 50
  }

  # Application Gateway
  appgw_sku_name = "WAF_v2"
  appgw_sku_tier = "WAF_v2"
  appgw_capacity = 2

  # API Management
  apim_publisher_name  = "AKS Starter Team"
  apim_publisher_email = "team@example.com"
  apim_sku_name        = "Developer_1"

  # Monitoring
  log_retention_days = 30
}
