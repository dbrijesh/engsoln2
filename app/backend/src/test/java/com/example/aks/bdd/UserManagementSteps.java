// Cucumber step definitions for User Management API
package com.example.aks.bdd;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import java.util.List;
import java.util.Map;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class UserManagementSteps {

  @LocalServerPort private int port;

  private Response response;
  private String authToken;
  private Long storedUserId;

  @Given("the application is running")
  public void theApplicationIsRunning() {
    RestAssured.port = port;
    RestAssured.baseURI = "http://localhost";
  }

  @Given("the database is clean")
  public void theDatabaseIsClean() {
    // In a real scenario, this would clean the database
    // For H2 in-memory database, this happens automatically on test startup
  }

  @Given("I am authenticated as {string}")
  public void iAmAuthenticatedAs(String username) {
    // For testing purposes, we'll mock authentication
    // In a real scenario, this would obtain a valid JWT token
    authToken = "mock-jwt-token-for-" + username;
  }

  @Given("I am not authenticated")
  public void iAmNotAuthenticated() {
    authToken = null;
  }

  @Given("a user exists with username {string} and email {string}")
  public void aUserExistsWithUsernameAndEmail(String username, String email) {
    // Create a user for testing
    String requestBody =
        String.format("{\"username\":\"%s\",\"email\":\"%s\"}", username, email);

    Response createResponse =
        RestAssured.given()
            .contentType("application/json")
            .body(requestBody)
            .when()
            .post("/api/users");

    if (createResponse.getStatusCode() == 201) {
      storedUserId = createResponse.jsonPath().getLong("id");
    }
  }

  @Given("the following users exist:")
  public void theFollowingUsersExist(DataTable dataTable) {
    List<Map<String, String>> users = dataTable.asMaps(String.class, String.class);

    for (Map<String, String> user : users) {
      String username = user.get("username");
      String email = user.get("email");
      aUserExistsWithUsernameAndEmail(username, email);
    }
  }

  @When("I create a user with the following details:")
  public void iCreateAUserWithTheFollowingDetails(DataTable dataTable) {
    Map<String, String> userDetails = dataTable.asMaps(String.class, String.class).get(0);

    String requestBody =
        String.format(
            "{\"username\":\"%s\",\"email\":\"%s\",\"fullName\":\"%s\"}",
            userDetails.get("username"),
            userDetails.get("email"),
            userDetails.get("fullName"));

    response =
        RestAssured.given().contentType("application/json").body(requestBody).post("/api/users");
  }

  @When("I create a user with username {string} and email {string}")
  public void iCreateAUserWithUsernameAndEmail(String username, String email) {
    String requestBody = String.format("{\"username\":\"%s\",\"email\":\"%s\"}", username, email);

    response =
        RestAssured.given().contentType("application/json").body(requestBody).post("/api/users");
  }

  @When("I attempt to create a user with username {string} and email {string}")
  public void iAttemptToCreateAUserWithUsernameAndEmail(String username, String email) {
    String requestBody = String.format("{\"username\":\"%s\",\"email\":\"%s\"}", username, email);

    response =
        RestAssured.given().contentType("application/json").body(requestBody).post("/api/users");
  }

  @When("I attempt to create a user with empty username and empty email")
  public void iAttemptToCreateAUserWithEmptyUsernameAndEmptyEmail() {
    String requestBody = "{\"username\":\"\",\"email\":\"\"}";

    response =
        RestAssured.given().contentType("application/json").body(requestBody).post("/api/users");
  }

  @When("I attempt to create a user without authentication")
  public void iAttemptToCreateAUserWithoutAuthentication() {
    String requestBody = "{\"username\":\"test\",\"email\":\"test@example.com\"}";

    response =
        RestAssured.given().contentType("application/json").body(requestBody).post("/api/users");
  }

  @When("I request the user by their ID")
  public void iRequestTheUserByTheirId() {
    assertNotNull(storedUserId, "No user ID stored");
    response = RestAssured.get("/api/users/" + storedUserId);
  }

  @When("I request the user by the stored ID")
  public void iRequestTheUserByTheStoredId() {
    iRequestTheUserByTheirId();
  }

  @When("I attempt to request the user by the stored ID")
  public void iAttemptToRequestTheUserByTheStoredId() {
    iRequestTheUserByTheirId();
  }

  @When("I request a user with ID {long}")
  public void iRequestAUserWithId(Long id) {
    response = RestAssured.get("/api/users/" + id);
  }

  @When("I request all users")
  public void iRequestAllUsers() {
    response = RestAssured.get("/api/users");
  }

  @When("I delete the user by their ID")
  public void iDeleteTheUserByTheirId() {
    assertNotNull(storedUserId, "No user ID stored");
    response = RestAssured.delete("/api/users/" + storedUserId);
  }

  @When("I delete the user by the stored ID")
  public void iDeleteTheUserByTheStoredId() {
    iDeleteTheUserByTheirId();
  }

  @When("I attempt to delete a user with ID {long}")
  public void iAttemptToDeleteAUserWithId(Long id) {
    response = RestAssured.delete("/api/users/" + id);
  }

  @Then("the response status code should be {int}")
  public void theResponseStatusCodeShouldBe(int statusCode) {
    assertEquals(statusCode, response.getStatusCode());
  }

  @Then("the response should contain the user details")
  public void theResponseShouldContainTheUserDetails() {
    response.then().body("username", notNullValue()).body("email", notNullValue());
  }

  @Then("the user should have a valid ID")
  public void theUserShouldHaveAValidId() {
    Long id = response.jsonPath().getLong("id");
    assertNotNull(id);
    assertTrue(id > 0);
  }

  @Then("the user should have a creation timestamp")
  public void theUserShouldHaveACreationTimestamp() {
    response.then().body("createdAt", notNullValue());
  }

  @Then("the response should contain username {string}")
  public void theResponseShouldContainUsername(String username) {
    response.then().body("username", equalTo(username));
  }

  @Then("the response should contain email {string}")
  public void theResponseShouldContainEmail(String email) {
    response.then().body("email", equalTo(email));
  }

  @Then("the response should contain {int} users")
  public void theResponseShouldContainUsers(int count) {
    List<Map<String, Object>> users = response.jsonPath().getList("$");
    assertEquals(count, users.size());
  }

  @Then("the error message should contain {string}")
  public void theErrorMessageShouldContain(String expectedMessage) {
    String actualMessage = response.jsonPath().getString("message");
    assertTrue(
        actualMessage.contains(expectedMessage),
        "Expected message to contain: "
            + expectedMessage
            + " but was: "
            + actualMessage);
  }

  @Then("the field error for {string} should be present")
  public void theFieldErrorForShouldBePresent(String fieldName) {
    Map<String, String> fieldErrors = response.jsonPath().getMap("fieldErrors");
    assertNotNull(fieldErrors, "Field errors should be present");
    assertTrue(
        fieldErrors.containsKey(fieldName),
        "Field error for " + fieldName + " should be present");
  }

  @Then("the user should no longer exist in the database")
  public void theUserShouldNoLongerExistInTheDatabase() {
    // Try to fetch the deleted user
    Response getResponse = RestAssured.get("/api/users/" + storedUserId);
    assertEquals(404, getResponse.getStatusCode());
  }

  @Then("I store the created user ID")
  public void iStoreTheCreatedUserId() {
    storedUserId = response.jsonPath().getLong("id");
    assertNotNull(storedUserId);
  }
}
