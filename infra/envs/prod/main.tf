# Terraform configuration for prod environment
terraform {
  required_version = ">= 1.5.0"

  backend "azurerm" {
    # Configure via: terraform init -backend-config="backend-prod.hcl"
  }
}

module "infrastructure" {
  source = "../../"

  project_name = "aksstarter"
  environment  = "prod"
  location     = "eastus"

  tags = {
    Project     = "AKS Starter Kit"
    Environment = "prod"
    ManagedBy   = "Terraform"
  }

  acr_sku            = "Premium"
  kubernetes_version = "1.27"

  aks_default_node_pool = {
    name                = "default"
    vm_size             = "Standard_D4s_v3"
    node_count          = 5
    min_count           = 5
    max_count           = 20
    enable_auto_scaling = true
    os_disk_size_gb     = 100
  }

  appgw_capacity       = 3
  apim_publisher_name  = "AKS Starter Team"
  apim_publisher_email = "team@example.com"
  apim_sku_name        = "Standard_1"
  log_retention_days   = 90
}
