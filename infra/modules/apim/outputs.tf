# Outputs for APIM module
output "apim_id" {
  description = "ID of the API Management service"
  value       = azurerm_api_management.apim.id
}

output "apim_name" {
  description = "Name of the API Management service"
  value       = azurerm_api_management.apim.name
}

output "gateway_url" {
  description = "Gateway URL for API Management"
  value       = azurerm_api_management.apim.gateway_url
}

output "portal_url" {
  description = "Developer portal URL"
  value       = azurerm_api_management.apim.developer_portal_url
}
