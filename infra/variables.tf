# Terraform variables for Azure infrastructure
variable "project_name" {
  description = "Project name used for resource naming"
  type        = string
  default     = "aksstarter"
}

variable "environment" {
  description = "Environment name (dev, stage, prod)"
  type        = string
}

variable "location" {
  description = "Azure region for resources"
  type        = string
  default     = "eastus"
}

variable "tags" {
  description = "Tags to apply to all resources"
  type        = map(string)
  default = {
    Project     = "AKS Starter Kit"
    ManagedBy   = "Terraform"
  }
}

# ACR Variables
variable "acr_sku" {
  description = "SKU for Azure Container Registry"
  type        = string
  default     = "Standard"
}

# AKS Variables
variable "kubernetes_version" {
  description = "Kubernetes version for AKS"
  type        = string
  default     = "1.27"
}

variable "aks_default_node_pool" {
  description = "Default node pool configuration"
  type = object({
    name                = string
    vm_size             = string
    node_count          = number
    min_count           = number
    max_count           = number
    enable_auto_scaling = bool
    os_disk_size_gb     = number
  })
  default = {
    name                = "default"
    vm_size             = "Standard_D2s_v3"
    node_count          = 2
    min_count           = 2
    max_count           = 5
    enable_auto_scaling = true
    os_disk_size_gb     = 50
  }
}

variable "aks_network_profile" {
  description = "AKS network profile configuration"
  type = object({
    network_plugin    = string
    network_policy    = string
    service_cidr      = string
    dns_service_ip    = string
    load_balancer_sku = string
  })
  default = {
    network_plugin    = "azure"
    network_policy    = "azure"
    service_cidr      = "10.0.0.0/16"
    dns_service_ip    = "10.0.0.10"
    load_balancer_sku = "standard"
  }
}

# Monitoring Variables
variable "log_retention_days" {
  description = "Log Analytics retention in days"
  type        = number
  default     = 30
}

# Application Gateway Variables
variable "appgw_sku_name" {
  description = "SKU name for Application Gateway"
  type        = string
  default     = "WAF_v2"
}

variable "appgw_sku_tier" {
  description = "SKU tier for Application Gateway"
  type        = string
  default     = "WAF_v2"
}

variable "appgw_capacity" {
  description = "Capacity for Application Gateway"
  type        = number
  default     = 2
}

# API Management Variables
variable "apim_publisher_name" {
  description = "Publisher name for API Management"
  type        = string
  default     = "AKS Starter Team"
}

variable "apim_publisher_email" {
  description = "Publisher email for API Management"
  type        = string
  default     = "team@example.com"
}

variable "apim_sku_name" {
  description = "SKU for API Management"
  type        = string
  default     = "Developer_1"
}
