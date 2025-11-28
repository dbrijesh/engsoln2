# Backend configuration for dev environment
# Usage: terraform init -backend-config="backend-dev.hcl"
resource_group_name  = "terraform-state-rg"
storage_account_name = "tfstateaksstarter"
container_name       = "tfstate"
key                  = "dev.terraform.tfstate"
