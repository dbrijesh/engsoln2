// Home page component with API call to backend
import React, { useState, useEffect } from 'react';
import { useMsal } from '@azure/msal-react';
import { apiRequest, apiConfig } from '../authConfig';
import axios from 'axios';
import './Home.css';

function Home() {
  const { instance, accounts } = useMsal();
  const [apiResponse, setApiResponse] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const callApi = async () => {
    setLoading(true);
    setError(null);

    try {
      // Acquire token silently
      const response = await instance.acquireTokenSilent({
        ...apiRequest,
        account: accounts[0],
      });

      // Call backend API with token
      const apiResult = await axios.get(`${apiConfig.apiEndpoint}/hello`, {
        headers: {
          Authorization: `Bearer ${response.accessToken}`,
        },
      });

      setApiResponse(apiResult.data);
    } catch (err) {
      if (err.name === 'InteractionRequiredAuthError') {
        // Fallback to interactive login if silent fails
        try {
          const response = await instance.acquireTokenPopup(apiRequest);
          const apiResult = await axios.get(`${apiConfig.apiEndpoint}/hello`, {
            headers: {
              Authorization: `Bearer ${response.accessToken}`,
            },
          });
          setApiResponse(apiResult.data);
        } catch (popupErr) {
          setError(popupErr.message);
        }
      } else {
        setError(err.response?.data?.message || err.message);
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="home-container">
      <h1>Hello World - AKS Starter Kit</h1>
      <p className="welcome-text">
        Welcome, {accounts[0]?.name || 'User'}! 👋
      </p>

      <div className="card">
        <h2>Test Backend API</h2>
        <p>Click the button below to call the Spring Boot backend:</p>

        <button
          onClick={callApi}
          disabled={loading}
          className="api-button"
        >
          {loading ? 'Calling API...' : 'Call /api/hello'}
        </button>

        {apiResponse && (
          <div className="response-success">
            <h3>✓ API Response:</h3>
            <pre>{JSON.stringify(apiResponse, null, 2)}</pre>
          </div>
        )}

        {error && (
          <div className="response-error">
            <h3>✗ Error:</h3>
            <p>{error}</p>
          </div>
        )}
      </div>

      <div className="info-section">
        <h3>🚀 What's Running</h3>
        <ul>
          <li>React 18 SPA with Azure Entra SSO (MSAL.js)</li>
          <li>Spring Boot 3.x REST API backend</li>
          <li>Deployed on Azure Kubernetes Service (AKS)</li>
          <li>Secured with HTTPS and WAF</li>
        </ul>
      </div>
    </div>
  );
}

export default Home;
