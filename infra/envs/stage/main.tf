# Terraform configuration for stage environment
terraform {
  required_version = ">= 1.5.0"

  backend "azurerm" {
    # Configure via: terraform init -backend-config="backend-stage.hcl"
  }
}

module "infrastructure" {
  source = "../../"

  project_name = "aksstarter"
  environment  = "stage"
  location     = "eastus"

  tags = {
    Project     = "AKS Starter Kit"
    Environment = "stage"
    ManagedBy   = "Terraform"
  }

  acr_sku            = "Standard"
  kubernetes_version = "1.27"

  aks_default_node_pool = {
    name                = "default"
    vm_size             = "Standard_D2s_v3"
    node_count          = 3
    min_count           = 3
    max_count           = 10
    enable_auto_scaling = true
    os_disk_size_gb     = 50
  }

  appgw_capacity       = 2
  apim_publisher_name  = "AKS Starter Team"
  apim_publisher_email = "team@example.com"
  apim_sku_name        = "Developer_1"
  log_retention_days   = 60
}
