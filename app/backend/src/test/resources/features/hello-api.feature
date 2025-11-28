# BDD feature file for Hello API
Feature: Hello API
  As an authenticated user
  I want to call the Hello API
  So that I can receive a personalized greeting

  Background:
    Given the application is running

  Scenario: Successful API call with authentication
    Given I am authenticated as "testuser"
    When I call the "/api/hello" endpoint
    Then I should receive a 200 status code
    And the response should contain my username
    And the response should contain a message field

  Scenario: API call without authentication
    Given I am not authenticated
    When I call the "/api/hello" endpoint
    Then I should receive a 401 status code

  Scenario: Public health endpoint
    When I call the "/api/public/health" endpoint
    Then I should receive a 200 status code
    And the response should be "OK"
