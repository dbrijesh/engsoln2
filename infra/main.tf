# Main Terraform configuration for Azure AKS infrastructure
terraform {
  required_version = ">= 1.5.0"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.80"
    }
    azuread = {
      source  = "hashicorp/azuread"
      version = "~> 2.45"
    }
  }

  backend "azurerm" {
    # Backend configuration provided via backend config file or CLI
    # Example: terraform init -backend-config="backend-dev.hcl"
  }
}

provider "azurerm" {
  features {
    key_vault {
      purge_soft_delete_on_destroy = false
    }
    resource_group {
      prevent_deletion_if_contains_resources = false
    }
  }
}

provider "azuread" {}

# Resource Group
resource "azurerm_resource_group" "main" {
  name     = "${var.project_name}-${var.environment}-rg"
  location = var.location

  tags = var.tags
}

# Azure Container Registry
module "acr" {
  source = "./modules/acr"

  name                = "${var.project_name}${var.environment}acr"
  resource_group_name = azurerm_resource_group.main.name
  location            = azurerm_resource_group.main.location
  sku                 = var.acr_sku
  tags                = var.tags
}

# Azure Key Vault
module "keyvault" {
  source = "./modules/keyvault"

  name                = "${var.project_name}-${var.environment}-kv"
  resource_group_name = azurerm_resource_group.main.name
  location            = azurerm_resource_group.main.location
  tenant_id           = data.azurerm_client_config.current.tenant_id
  tags                = var.tags
}

# AKS Cluster
module "aks" {
  source = "./modules/aks"

  cluster_name        = "${var.project_name}-${var.environment}-aks"
  resource_group_name = azurerm_resource_group.main.name
  location            = azurerm_resource_group.main.location
  dns_prefix          = "${var.project_name}-${var.environment}"
  kubernetes_version  = var.kubernetes_version

  default_node_pool = var.aks_default_node_pool
  identity_type     = "SystemAssigned"

  network_profile = var.aks_network_profile

  tags = var.tags

  depends_on = [module.acr]
}

# Grant AKS access to ACR
resource "azurerm_role_assignment" "aks_acr_pull" {
  principal_id         = module.aks.kubelet_identity_object_id
  role_definition_name = "AcrPull"
  scope                = module.acr.acr_id
}

# Log Analytics Workspace
module "monitoring" {
  source = "./modules/monitoring"

  name                = "${var.project_name}-${var.environment}-law"
  resource_group_name = azurerm_resource_group.main.name
  location            = azurerm_resource_group.main.location
  retention_days      = var.log_retention_days
  tags                = var.tags
}

# Application Gateway with WAF
module "appgw_waf" {
  source = "./modules/appgw_waf"

  name                = "${var.project_name}-${var.environment}-appgw"
  resource_group_name = azurerm_resource_group.main.name
  location            = azurerm_resource_group.main.location
  sku_name            = var.appgw_sku_name
  sku_tier            = var.appgw_sku_tier
  capacity            = var.appgw_capacity

  tags = var.tags
}

# API Management
module "apim" {
  source = "./modules/apim"

  name                = "${var.project_name}-${var.environment}-apim"
  resource_group_name = azurerm_resource_group.main.name
  location            = azurerm_resource_group.main.location
  publisher_name      = var.apim_publisher_name
  publisher_email     = var.apim_publisher_email
  sku_name            = var.apim_sku_name

  tags = var.tags
}

# Data source for current Azure config
data "azurerm_client_config" "current" {}
