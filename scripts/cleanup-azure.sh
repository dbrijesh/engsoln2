#!/bin/bash
# Script to cleanup Azure resources

set -e

ENVIRONMENT=${1:-dev}

echo "⚠️  Azure Resource Cleanup"
echo "=========================="
echo ""
echo "Environment: $ENVIRONMENT"
echo ""
echo "This will DESTROY all Azure resources for the $ENVIRONMENT environment!"
echo "This action cannot be undone."
echo ""

read -p "Are you sure you want to continue? (type 'yes' to confirm): " confirmation

if [ "$confirmation" != "yes" ]; then
    echo "Cleanup cancelled."
    exit 0
fi

echo ""
echo "🗑️  Destroying Terraform resources..."
echo "------------------------------------"

cd infra/envs/$ENVIRONMENT

# Initialize Terraform
terraform init

# Destroy resources
terraform destroy -auto-approve

echo ""
echo "✅ Terraform resources destroyed!"
echo ""
echo "💡 Note: The following may still exist and incur costs:"
echo "  - Terraform state storage account"
echo "  - Log Analytics workspace data (retained per retention policy)"
echo "  - Key Vault (soft-deleted, recoverable for retention period)"
echo ""
echo "To completely remove all traces:"
echo "  1. Delete soft-deleted Key Vault:"
echo "     az keyvault purge --name aksstarter-$ENVIRONMENT-kv"
echo ""
echo "  2. Delete Terraform state (if no longer needed):"
echo "     az storage container delete --name tfstate --account-name tfstateaksstarter"
echo ""
echo "🎉 Cleanup complete!"
