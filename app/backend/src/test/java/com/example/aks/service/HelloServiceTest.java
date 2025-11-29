// Unit tests for HelloService
package com.example.aks.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.aks.dto.HelloResponse;

class HelloServiceTest {

  private HelloService helloService;

  @BeforeEach
  void setUp() {
    helloService = new HelloService();
    ReflectionTestUtils.setField(helloService, "appVersion", "1.0.0");
  }

  @Test
  void getHelloMessage_ShouldReturnValidResponse() {
    String username = "testuser";

    HelloResponse response = helloService.getHelloMessage(username);

    assertNotNull(response);
    assertEquals(username, response.getUser());
    assertTrue(response.getMessage().contains(username));
    assertEquals("1.0.0", response.getVersion());
    assertNotNull(response.getTimestamp());
  }

  @Test
  void getHelloMessage_WithDifferentUsernames_ShouldReturnCustomizedMessages() {
    HelloResponse response1 = helloService.getHelloMessage("alice");
    HelloResponse response2 = helloService.getHelloMessage("bob");

    assertNotEquals(response1.getUser(), response2.getUser());
    assertTrue(response1.getMessage().contains("alice"));
    assertTrue(response2.getMessage().contains("bob"));
  }

  @Test
  void getFallbackHelloMessage_ShouldReturnFallbackResponse() {
    String username = "testuser";
    Exception testException = new RuntimeException("Test exception");

    HelloResponse response = helloService.getFallbackHelloMessage(username, testException);

    assertNotNull(response);
    assertEquals(username, response.getUser());
    assertTrue(response.getMessage().contains("Fallback"));
    assertTrue(response.getVersion().contains("fallback"));
  }
}
