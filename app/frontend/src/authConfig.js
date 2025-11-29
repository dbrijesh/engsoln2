// Helper to get env variable from window._env_ (runtime) or process.env (build-time)
const getEnv = (key) => {
  return (window._env_ && window._env_[key]) || process.env[key];
};

// MSAL configuration for Azure Entra authentication
export const msalConfig = {
  auth: {
    clientId: getEnv('REACT_APP_CLIENT_ID') || 'YOUR_CLIENT_ID',
    authority: `https://login.microsoftonline.com/${getEnv('REACT_APP_TENANT_ID') || 'YOUR_TENANT_ID'}`,
    redirectUri: window.location.origin,
    postLogoutRedirectUri: window.location.origin,
  },
  cache: {
    cacheLocation: 'sessionStorage',
    storeAuthStateInCookie: false,
  },
  system: {
    loggerOptions: {
      loggerCallback: (level, message, containsPii) => {
        if (containsPii) return;
        switch (level) {
          case 0: // Error
            console.error(message);
            break;
          case 1: // Warning
            console.warn(message);
            break;
          case 2: // Info
            console.info(message);
            break;
          case 3: // Verbose
            console.debug(message);
            break;
          default:
            console.log(message);
        }
      },
    },
  },
};

// Scopes for API access
export const loginRequest = {
  scopes: ['User.Read', 'openid', 'profile', 'email'],
};

export const apiRequest = {
  scopes: [
    getEnv('REACT_APP_API_SCOPE') || 'api://YOUR_API_CLIENT_ID/access_as_user',
  ],
};

// Graph API endpoint for user profile
export const graphConfig = {
  graphMeEndpoint: 'https://graph.microsoft.com/v1.0/me',
};

// Backend API endpoint
export const apiConfig = {
  apiEndpoint: getEnv('REACT_APP_API_URL') || 'http://localhost:8080/api',
};
