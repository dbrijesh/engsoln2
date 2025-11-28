// Unit tests for Home component
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import Home from './Home';
import axios from 'axios';

// Mock MSAL
jest.mock('@azure/msal-react', () => ({
  useMsal: () => ({
    instance: {
      acquireTokenSilent: jest.fn().mockResolvedValue({
        accessToken: 'mock-token',
      }),
    },
    accounts: [{ name: 'Test User', username: 'test@example.com' }],
  }),
}));

// Mock axios
jest.mock('axios');

describe('Home Component', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('renders Home component with welcome message', () => {
    render(<Home />);
    expect(screen.getByText(/Hello World - AKS Starter Kit/i)).toBeInTheDocument();
    expect(screen.getByText(/Welcome, Test User!/i)).toBeInTheDocument();
  });

  test('displays Call API button', () => {
    render(<Home />);
    expect(screen.getByText(/Call \/api\/hello/i)).toBeInTheDocument();
  });

  test('calls backend API when button is clicked', async () => {
    const mockResponse = { data: { message: 'Hello, Test User' } };
    axios.get.mockResolvedValue(mockResponse);

    render(<Home />);
    const button = screen.getByText(/Call \/api\/hello/i);

    fireEvent.click(button);

    await waitFor(() => {
      expect(axios.get).toHaveBeenCalled();
    });
  });

  test('displays API response on successful call', async () => {
    const mockResponse = { data: { message: 'Hello, Test User' } };
    axios.get.mockResolvedValue(mockResponse);

    render(<Home />);
    const button = screen.getByText(/Call \/api\/hello/i);

    fireEvent.click(button);

    await waitFor(() => {
      expect(screen.getByText(/API Response:/i)).toBeInTheDocument();
    });
  });

  test('displays error message on API failure', async () => {
    axios.get.mockRejectedValue(new Error('Network error'));

    render(<Home />);
    const button = screen.getByText(/Call \/api\/hello/i);

    fireEvent.click(button);

    await waitFor(() => {
      expect(screen.getByText(/Error:/i)).toBeInTheDocument();
    });
  });
});
