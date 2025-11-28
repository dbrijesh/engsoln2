# Outputs for Key Vault module
output "keyvault_id" {
  description = "ID of the Key Vault"
  value       = azurerm_key_vault.kv.id
}

output "keyvault_name" {
  description = "Name of the Key Vault"
  value       = azurerm_key_vault.kv.name
}

output "keyvault_uri" {
  description = "URI of the Key Vault"
  value       = azurerm_key_vault.kv.vault_uri
}
