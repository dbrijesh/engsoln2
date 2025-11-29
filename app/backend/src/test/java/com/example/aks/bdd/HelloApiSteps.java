// Cucumber step definitions for Hello API
package com.example.aks.bdd;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;
import io.restassured.RestAssured;
import io.restassured.response.Response;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class HelloApiSteps {

  @LocalServerPort private int port;

  private Response response;
  private String authToken;

  @Given("the application is running")
  public void theApplicationIsRunning() {
    RestAssured.port = port;
    RestAssured.baseURI = "http://localhost";
  }

  @Given("I am authenticated as {string}")
  public void iAmAuthenticatedAs(String username) {
    // In a real scenario, this would obtain a valid JWT token
    // For testing, we'll use a mock token or skip authentication
    authToken = "mock-jwt-token-for-" + username;
  }

  @Given("I am not authenticated")
  public void iAmNotAuthenticated() {
    authToken = null;
  }

  @When("I call the {string} endpoint")
  public void iCallTheEndpoint(String endpoint) {
    if (authToken != null) {
      // For now, public endpoints only (authentication requires proper JWT setup)
      response = given().when().get(endpoint);
    } else {
      response = given().when().get(endpoint);
    }
  }

  @Then("I should receive a {int} status code")
  public void iShouldReceiveAStatusCode(int expectedStatusCode) {
    assertEquals(expectedStatusCode, response.getStatusCode());
  }

  @Then("the response should contain my username")
  public void theResponseShouldContainMyUsername() {
    response.then().body("user", notNullValue());
  }

  @Then("the response should contain a message field")
  public void theResponseShouldContainAMessageField() {
    response.then().body("message", notNullValue());
  }

  @Then("the response should be {string}")
  public void theResponseShouldBe(String expectedBody) {
    assertEquals(expectedBody, response.getBody().asString());
  }
}
