# Variables for APIM module
variable "name" {
  description = "Name of the API Management service"
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

variable "publisher_name" {
  description = "Publisher name"
  type        = string
}

variable "publisher_email" {
  description = "Publisher email"
  type        = string
}

variable "sku_name" {
  description = "SKU name (Developer_1, Standard_1, Premium_1)"
  type        = string
  default     = "Developer_1"
}

variable "tags" {
  description = "Tags to apply to resources"
  type        = map(string)
  default     = {}
}
