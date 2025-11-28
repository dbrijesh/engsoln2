# Terraform outputs for Azure infrastructure
output "resource_group_name" {
  description = "Name of the resource group"
  value       = azurerm_resource_group.main.name
}

output "aks_cluster_name" {
  description = "Name of the AKS cluster"
  value       = module.aks.cluster_name
}

output "aks_cluster_id" {
  description = "ID of the AKS cluster"
  value       = module.aks.cluster_id
}

output "aks_kube_config_raw" {
  description = "Raw kubeconfig for AKS cluster"
  value       = module.aks.kube_config_raw
  sensitive   = true
}

output "acr_login_server" {
  description = "Login server for Azure Container Registry"
  value       = module.acr.acr_login_server
}

output "acr_name" {
  description = "Name of Azure Container Registry"
  value       = module.acr.acr_name
}

output "keyvault_name" {
  description = "Name of Azure Key Vault"
  value       = module.keyvault.keyvault_name
}

output "keyvault_uri" {
  description = "URI of Azure Key Vault"
  value       = module.keyvault.keyvault_uri
}

output "log_analytics_workspace_id" {
  description = "ID of Log Analytics workspace"
  value       = module.monitoring.workspace_id
}

output "appgw_public_ip" {
  description = "Public IP of Application Gateway"
  value       = module.appgw_waf.public_ip_address
}

output "apim_gateway_url" {
  description = "Gateway URL for API Management"
  value       = module.apim.gateway_url
}
