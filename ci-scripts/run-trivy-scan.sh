#!/bin/bash
# Script to run Trivy container vulnerability scanning

set -e

IMAGE_NAME=$1
OUTPUT_FILE=${2:-trivy-results.json}

if [ -z "$IMAGE_NAME" ]; then
    echo "Usage: $0 <image-name> [output-file]"
    exit 1
fi

echo "Scanning image: $IMAGE_NAME"

# Install Trivy if not present
if ! command -v trivy &> /dev/null; then
    echo "Installing Trivy..."
    wget -qO - https://aquasecurity.github.io/trivy-repo/deb/public.key | sudo apt-key add -
    echo "deb https://aquasecurity.github.io/trivy-repo/deb $(lsb_release -sc) main" | sudo tee -a /etc/apt/sources.list.d/trivy.list
    sudo apt-get update
    sudo apt-get install trivy -y
fi

# Run Trivy scan
echo "Running Trivy scan..."
trivy image --exit-code 0 --severity HIGH,CRITICAL --format json --output "$OUTPUT_FILE" "$IMAGE_NAME"

echo "Scan complete. Results saved to $OUTPUT_FILE"

# Display summary
echo ""
echo "Summary:"
trivy image --severity HIGH,CRITICAL --format table "$IMAGE_NAME"

# Fail if CRITICAL vulnerabilities found (configurable)
if [ "${FAIL_ON_CRITICAL:-true}" == "true" ]; then
    trivy image --exit-code 1 --severity CRITICAL "$IMAGE_NAME"
fi
