// MSAL configuration for Azure Entra authentication
export const msalConfig = {
  auth: {
    clientId: process.env.REACT_APP_CLIENT_ID || 'YOUR_CLIENT_ID',
    authority: process.env.REACT_APP_AUTHORITY || 'https://login.microsoftonline.com/YOUR_TENANT_ID',
    redirectUri: process.env.REACT_APP_REDIRECT_URI || window.location.origin,
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
    process.env.REACT_APP_API_SCOPE || 'api://YOUR_API_CLIENT_ID/access_as_user',
  ],
};

// Graph API endpoint for user profile
export const graphConfig = {
  graphMeEndpoint: 'https://graph.microsoft.com/v1.0/me',
};

// Backend API endpoint
export const apiConfig = {
  apiEndpoint: process.env.REACT_APP_API_URL || 'http://localhost:8080/api',
};
