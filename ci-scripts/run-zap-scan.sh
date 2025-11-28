#!/bin/bash
# Script to run OWASP ZAP DAST scanning

set -e

TARGET_URL=$1
REPORT_NAME=${2:-zap-report}

if [ -z "$TARGET_URL" ]; then
    echo "Usage: $0 <target-url> [report-name]"
    exit 1
fi

echo "Running OWASP ZAP scan on: $TARGET_URL"

# Create reports directory
mkdir -p zap-reports

# Run ZAP baseline scan
docker run -v $(pwd)/zap-reports:/zap/wrk/:rw \
  -t owasp/zap2docker-stable zap-baseline.py \
  -t "$TARGET_URL" \
  -r "$REPORT_NAME.html" \
  -J "$REPORT_NAME.json" \
  -w "$REPORT_NAME.md" \
  -c zap-baseline-config.conf || true

echo "ZAP scan complete. Reports saved to zap-reports/"
echo "  - HTML: zap-reports/$REPORT_NAME.html"
echo "  - JSON: zap-reports/$REPORT_NAME.json"
echo "  - Markdown: zap-reports/$REPORT_NAME.md"
