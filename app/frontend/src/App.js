// Main React application component with MSAL authentication wrapper
import React from 'react';
import { MsalProvider, AuthenticatedTemplate, UnauthenticatedTemplate } from '@azure/msal-react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import './App.css';
import Home from './components/Home';
import Profile from './components/Profile';
import Navigation from './components/Navigation';
import LoginPage from './components/LoginPage';

function App({ msalInstance }) {
  return (
    <MsalProvider instance={msalInstance}>
      <Router>
        <div className="App">
          <AuthenticatedTemplate>
            <Navigation />
            <Routes>
              <Route path="/" element={<Home />} />
              <Route path="/profile" element={<Profile />} />
            </Routes>
          </AuthenticatedTemplate>

          <UnauthenticatedTemplate>
            <LoginPage />
          </UnauthenticatedTemplate>
        </div>
      </Router>
    </MsalProvider>
  );
}

export default App;
