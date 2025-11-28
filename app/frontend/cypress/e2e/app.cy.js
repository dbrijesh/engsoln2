// E2E tests for the application
describe('AKS Starter Kit - E2E Tests', () => {
  beforeEach(() => {
    cy.visit('/');
  });

  it('should load the application', () => {
    cy.get('.App').should('exist');
  });

  // Note: These tests require proper MSAL configuration and Azure Entra setup
  // For local testing, you may need to mock authentication

  it('should display login page when not authenticated', () => {
    // This test assumes the user is not logged in
    cy.contains('Sign In').should('be.visible');
  });

  // Example authenticated test (requires manual login or automation setup)
  it.skip('should display home page after authentication', () => {
    // This would require MSAL authentication flow
    // In a real scenario, you'd use cy.session() or custom commands
    // to handle Azure Entra login programmatically
    cy.contains('Hello World - AKS Starter Kit').should('be.visible');
  });

  it.skip('should call backend API successfully', () => {
    // Requires authentication
    cy.get('.api-button').click();
    cy.contains('API Response:', { timeout: 10000 }).should('be.visible');
    cy.get('.response-success pre').should('contain', 'message');
  });

  it.skip('should navigate to profile page', () => {
    // Requires authentication
    cy.contains('Profile').click();
    cy.contains('User Profile').should('be.visible');
  });

  it.skip('should logout successfully', () => {
    // Requires authentication
    cy.contains('Logout').click();
    cy.contains('Sign In').should('be.visible');
  });
});

// Custom command example for MSAL authentication (to be implemented)
// Cypress.Commands.add('loginAzureAD', (username, password) => {
//   // Implement Azure AD login flow
//   // This typically involves intercepting MSAL requests or using the MSAL API
// });
