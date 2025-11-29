#!/bin/sh

# Generate env-config.js from environment variables at runtime
cat <<EOF > /usr/share/nginx/html/env-config.js
window._env_ = {
  REACT_APP_CLIENT_ID: "${REACT_APP_CLIENT_ID}",
  REACT_APP_TENANT_ID: "${REACT_APP_TENANT_ID}",
  REACT_APP_API_URL: "${REACT_APP_API_URL}",
  REACT_APP_API_SCOPE: "${REACT_APP_API_SCOPE}"
};
EOF

# Start nginx
exec nginx -g 'daemon off;'
