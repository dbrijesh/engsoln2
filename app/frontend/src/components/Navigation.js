// Navigation bar component with login/logout functionality
import React from 'react';
import { useMsal } from '@azure/msal-react';
import { Link } from 'react-router-dom';
import './Navigation.css';

function Navigation() {
  const { instance } = useMsal();

  const handleLogout = () => {
    instance.logoutRedirect({
      postLogoutRedirectUri: '/',
    });
  };

  return (
    <nav className="navigation">
      <div className="nav-container">
        <div className="nav-brand">
          <Link to="/">AKS Starter Kit</Link>
        </div>

        <div className="nav-links">
          <Link to="/" className="nav-link">Home</Link>
          <Link to="/profile" className="nav-link">Profile</Link>
          <button onClick={handleLogout} className="logout-button">
            Logout
          </button>
        </div>
      </div>
    </nav>
  );
}

export default Navigation;
