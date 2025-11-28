# BDD feature file for User Management API
Feature: User Management API
  As an API consumer
  I want to manage users through REST endpoints
  So that I can create, retrieve, and delete user accounts

  Background:
    Given the application is running
    And the database is clean

  @smoke @positive
  Scenario: Create a new user with valid data
    Given I am authenticated as "admin"
    When I create a user with the following details:
      | username | email               | fullName  |
      | john_doe | john@example.com    | John Doe  |
    Then the response status code should be 201
    And the response should contain the user details
    And the user should have a valid ID
    And the user should have a creation timestamp

  @positive
  Scenario: Retrieve an existing user by ID
    Given I am authenticated as "admin"
    And a user exists with username "jane_doe" and email "jane@example.com"
    When I request the user by their ID
    Then the response status code should be 200
    And the response should contain username "jane_doe"
    And the response should contain email "jane@example.com"

  @positive
  Scenario: Retrieve all users
    Given I am authenticated as "admin"
    And the following users exist:
      | username    | email                  |
      | alice_smith | alice@example.com      |
      | bob_jones   | bob@example.com        |
      | carol_white | carol@example.com      |
    When I request all users
    Then the response status code should be 200
    And the response should contain 3 users

  @positive
  Scenario: Delete an existing user
    Given I am authenticated as "admin"
    And a user exists with username "to_delete" and email "delete@example.com"
    When I delete the user by their ID
    Then the response status code should be 204
    And the user should no longer exist in the database

  @negative @validation
  Scenario: Create user with invalid username (too short)
    Given I am authenticated as "admin"
    When I attempt to create a user with username "ab" and email "test@example.com"
    Then the response status code should be 400
    And the error message should contain "Username must be between 3 and 50 characters"
    And the field error for "username" should be present

  @negative @validation
  Scenario: Create user with invalid email format
    Given I am authenticated as "admin"
    When I attempt to create a user with username "valid_user" and email "invalid-email"
    Then the response status code should be 400
    And the error message should contain "Email must be valid"
    And the field error for "email" should be present

  @negative @validation
  Scenario Outline: Create user with various invalid usernames
    Given I am authenticated as "admin"
    When I attempt to create a user with username "<username>" and email "test@example.com"
    Then the response status code should be 400
    And the error message should contain "<error_message>"

    Examples:
      | username       | error_message                                    |
      | ab             | Username must be between 3 and 50 characters     |
      |                | Username is required                             |
      | user@name      | Username can only contain alphanumeric characters|
      | user name      | Username can only contain alphanumeric characters|

  @negative @validation
  Scenario: Create user with missing required fields
    Given I am authenticated as "admin"
    When I attempt to create a user with empty username and empty email
    Then the response status code should be 400
    And the field error for "username" should be present
    And the field error for "email" should be present

  @negative @business
  Scenario: Create user with duplicate username
    Given I am authenticated as "admin"
    And a user exists with username "existing_user" and email "existing@example.com"
    When I attempt to create a user with username "existing_user" and email "new@example.com"
    Then the response status code should be 422
    And the error message should contain "already taken"

  @negative
  Scenario: Retrieve non-existent user by ID
    Given I am authenticated as "admin"
    When I request a user with ID 999999
    Then the response status code should be 404
    And the error message should contain "User not found"

  @negative
  Scenario: Delete non-existent user
    Given I am authenticated as "admin"
    When I attempt to delete a user with ID 999999
    Then the response status code should be 404
    And the error message should contain "User not found"

  @negative @validation
  Scenario: Request user with invalid ID format
    Given I am authenticated as "admin"
    When I request a user with ID 0
    Then the response status code should be 400

  @security
  Scenario: Access protected endpoint without authentication
    Given I am not authenticated
    When I attempt to create a user without authentication
    Then the response status code should be 401
    And the error message should contain "Unauthorized"

  @smoke @positive
  Scenario: End-to-end user lifecycle
    Given I am authenticated as "admin"
    When I create a user with username "lifecycle_user" and email "lifecycle@example.com"
    Then the response status code should be 201
    And I store the created user ID
    When I request the user by the stored ID
    Then the response status code should be 200
    And the response should contain username "lifecycle_user"
    When I delete the user by the stored ID
    Then the response status code should be 204
    When I attempt to request the user by the stored ID
    Then the response status code should be 404
