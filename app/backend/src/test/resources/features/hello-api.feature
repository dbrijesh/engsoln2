# BDD feature file for Hello API
Feature: Hello API
  As a user
  I want to call the Hello API
  So that I can verify the application is working

  Background:
    Given the application is running

  Scenario: Public health endpoint returns OK
    When I call the "/api/public/health" endpoint
    Then I should receive a 200 status code
    And the response should be "OK"

  Scenario: Hello endpoint returns greeting message
    When I call the "/api/hello" endpoint
    Then I should receive a 200 status code
    And the response should contain a message field
    And the response should contain a user field
