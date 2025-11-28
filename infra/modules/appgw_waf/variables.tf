# Variables for Application Gateway WAF module
variable "name" {
  description = "Name of the Application Gateway"
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

variable "sku_name" {
  description = "SKU name for Application Gateway"
  type        = string
  default     = "WAF_v2"
}

variable "sku_tier" {
  description = "SKU tier for Application Gateway"
  type        = string
  default     = "WAF_v2"
}

variable "capacity" {
  description = "Capacity for Application Gateway"
  type        = number
  default     = 2
}

variable "tags" {
  description = "Tags to apply to resources"
  type        = map(string)
  default     = {}
}
