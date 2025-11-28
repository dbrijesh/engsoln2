# Azure Kubernetes Service module
resource "azurerm_kubernetes_cluster" "aks" {
  name                = var.cluster_name
  location            = var.location
  resource_group_name = var.resource_group_name
  dns_prefix          = var.dns_prefix
  kubernetes_version  = var.kubernetes_version

  default_node_pool {
    name                = var.default_node_pool.name
    vm_size             = var.default_node_pool.vm_size
    node_count          = var.default_node_pool.node_count
    min_count           = var.default_node_pool.enable_auto_scaling ? var.default_node_pool.min_count : null
    max_count           = var.default_node_pool.enable_auto_scaling ? var.default_node_pool.max_count : null
    enable_auto_scaling = var.default_node_pool.enable_auto_scaling
    os_disk_size_gb     = var.default_node_pool.os_disk_size_gb
    type                = "VirtualMachineScaleSets"
  }

  identity {
    type = var.identity_type
  }

  network_profile {
    network_plugin    = var.network_profile.network_plugin
    network_policy    = var.network_profile.network_policy
    service_cidr      = var.network_profile.service_cidr
    dns_service_ip    = var.network_profile.dns_service_ip
    load_balancer_sku = var.network_profile.load_balancer_sku
  }

  # Enable Azure AD integration
  azure_active_directory_role_based_access_control {
    managed            = true
    azure_rbac_enabled = true
  }

  # Enable monitoring
  oms_agent {
    log_analytics_workspace_id = var.log_analytics_workspace_id
  }

  tags = var.tags
}

# Key Vault Secrets Provider addon
resource "azurerm_kubernetes_cluster_extension" "keyvault_secrets_provider" {
  count = var.enable_secret_store_csi_driver ? 1 : 0

  name              = "akvsecretsprovider"
  cluster_id        = azurerm_kubernetes_cluster.aks.id
  extension_type    = "Microsoft.AzureKeyVaultSecretsProvider"
  release_namespace = "kube-system"
}
