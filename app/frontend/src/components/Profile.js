// User profile component displaying authenticated user information
import React, { useState, useEffect } from 'react';
import { useMsal } from '@azure/msal-react';
import { loginRequest, graphConfig } from '../authConfig';
import axios from 'axios';
import './Profile.css';

function Profile() {
  const { instance, accounts } = useMsal();
  const [graphData, setGraphData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchProfile();
  }, []);

  const fetchProfile = async () => {
    try {
      const response = await instance.acquireTokenSilent({
        ...loginRequest,
        account: accounts[0],
      });

      const graphResponse = await axios.get(graphConfig.graphMeEndpoint, {
        headers: {
          Authorization: `Bearer ${response.accessToken}`,
        },
      });

      setGraphData(graphResponse.data);
    } catch (error) {
      console.error('Error fetching profile:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <div className="profile-container">Loading profile...</div>;
  }

  return (
    <div className="profile-container">
      <h1>User Profile</h1>

      <div className="profile-card">
        <h2>Account Information</h2>
        <div className="profile-info">
          <p><strong>Name:</strong> {accounts[0]?.name}</p>
          <p><strong>Username:</strong> {accounts[0]?.username}</p>
          <p><strong>Environment:</strong> {accounts[0]?.environment}</p>
        </div>
      </div>

      {graphData && (
        <div className="profile-card">
          <h2>Microsoft Graph Data</h2>
          <div className="profile-info">
            <p><strong>Display Name:</strong> {graphData.displayName}</p>
            <p><strong>Email:</strong> {graphData.mail || graphData.userPrincipalName}</p>
            <p><strong>Job Title:</strong> {graphData.jobTitle || 'N/A'}</p>
            <p><strong>Office Location:</strong> {graphData.officeLocation || 'N/A'}</p>
          </div>
        </div>
      )}

      <div className="profile-card">
        <h2>Token Claims</h2>
        <pre className="claims-data">
          {JSON.stringify(accounts[0]?.idTokenClaims, null, 2)}
        </pre>
      </div>
    </div>
  );
}

export default Profile;
