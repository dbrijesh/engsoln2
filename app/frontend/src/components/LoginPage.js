// Login page component for unauthenticated users
import React from 'react';
import { useMsal } from '@azure/msal-react';
import { loginRequest } from '../authConfig';
import './LoginPage.css';

function LoginPage() {
  const { instance } = useMsal();

  const handleLogin = (loginType) => {
    if (loginType === 'popup') {
      instance.loginPopup(loginRequest).catch((e) => {
        console.error('Login failed:', e);
      });
    } else if (loginType === 'redirect') {
      instance.loginRedirect(loginRequest).catch((e) => {
        console.error('Login failed:', e);
      });
    }
  };

  return (
    <div className="login-page">
      <div className="login-container">
        <div className="login-header">
          <h1>🚀 AKS Starter Kit</h1>
          <p className="subtitle">Production-Ready Azure Deployment Solution</p>
        </div>

        <div className="login-card">
          <h2>Welcome!</h2>
          <p>Sign in with your Azure Entra (Azure AD) account to continue.</p>

          <div className="login-buttons">
            <button
              onClick={() => handleLogin('redirect')}
              className="login-button primary"
            >
              Sign In with Redirect
            </button>

            <button
              onClick={() => handleLogin('popup')}
              className="login-button secondary"
            >
              Sign In with Popup
            </button>
          </div>

          <div className="login-info">
            <h3>Features:</h3>
            <ul>
              <li>✓ React 18 with Azure Entra SSO</li>
              <li>✓ Spring Boot 3.x REST API</li>
              <li>✓ Kubernetes on Azure AKS</li>
              <li>✓ Complete CI/CD with Azure DevOps</li>
              <li>✓ Security scanning & testing</li>
            </ul>
          </div>
        </div>

        <div className="login-footer">
          <p>Secured with Microsoft Authentication Library (MSAL)</p>
        </div>
      </div>
    </div>
  );
}

export default LoginPage;
