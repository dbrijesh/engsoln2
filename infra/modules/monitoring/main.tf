# Azure Monitor and Log Analytics module
resource "azurerm_log_analytics_workspace" "law" {
  name                = var.name
  location            = var.location
  resource_group_name = var.resource_group_name
  sku                 = var.sku
  retention_in_days   = var.retention_days

  tags = var.tags
}

# Application Insights (optional)
resource "azurerm_application_insights" "appinsights" {
  count = var.enable_application_insights ? 1 : 0

  name                = "${var.name}-appinsights"
  location            = var.location
  resource_group_name = var.resource_group_name
  workspace_id        = azurerm_log_analytics_workspace.law.id
  application_type    = "web"

  tags = var.tags
}
