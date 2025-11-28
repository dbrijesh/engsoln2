# Variables for AKS module
variable "cluster_name" {
  description = "Name of the AKS cluster"
  type        = string
}

variable "resource_group_name" {
  description = "Name of the resource group"
  type        = string
}

variable "location" {
  description = "Azure region"
  type        = string
}

variable "dns_prefix" {
  description = "DNS prefix for the AKS cluster"
  type        = string
}

variable "kubernetes_version" {
  description = "Kubernetes version"
  type        = string
}

variable "default_node_pool" {
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
}

variable "identity_type" {
  description = "Type of managed identity"
  type        = string
  default     = "SystemAssigned"
}

variable "network_profile" {
  description = "Network profile configuration"
  type = object({
    network_plugin    = string
    network_policy    = string
    service_cidr      = string
    dns_service_ip    = string
    load_balancer_sku = string
  })
}

variable "log_analytics_workspace_id" {
  description = "Log Analytics workspace ID for monitoring"
  type        = string
  default     = null
}

variable "enable_secret_store_csi_driver" {
  description = "Enable Key Vault secrets CSI driver"
  type        = bool
  default     = true
}

variable "tags" {
  description = "Tags to apply to resources"
  type        = map(string)
  default     = {}
}
