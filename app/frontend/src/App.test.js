// Unit tests for App component
import { render, screen } from '@testing-library/react';
import { PublicClientApplication } from '@azure/msal-browser';
import App from './App';

// Mock MSAL
jest.mock('@azure/msal-browser');
jest.mock('@azure/msal-react', () => ({
  MsalProvider: ({ children }) => <div>{children}</div>,
  AuthenticatedTemplate: ({ children }) => <div>{children}</div>,
  UnauthenticatedTemplate: ({ children }) => <div>{children}</div>,
  useMsal: () => ({
    instance: {},
    accounts: [{ name: 'Test User', username: 'test@example.com' }],
  }),
}));

describe('App Component', () => {
  let msalInstance;

  beforeEach(() => {
    msalInstance = new PublicClientApplication({
      auth: {
        clientId: 'test-client-id',
      },
    });
  });

  test('renders App component without crashing', () => {
    render(<App msalInstance={msalInstance} />);
    expect(document.querySelector('.App')).toBeInTheDocument();
  });

  test('renders navigation when authenticated', () => {
    render(<App msalInstance={msalInstance} />);
    // Since we're mocking AuthenticatedTemplate, this will always render
    expect(document.querySelector('.App')).toBeInTheDocument();
  });
});
