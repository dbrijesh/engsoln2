// Unit tests for HelloController
package com.example.aks.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.example.aks.config.LocalSecurityConfig;
import com.example.aks.dto.HelloResponse;
import com.example.aks.service.HelloService;

@WebMvcTest(HelloController.class)
@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.context.annotation.Import(LocalSecurityConfig.class)
class HelloControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private HelloService helloService;

  @Test
  void publicHealthEndpoint_ShouldReturnOk() throws Exception {
    mockMvc
        .perform(get("/api/public/health"))
        .andExpect(status().isOk())
        .andExpect(content().string("OK"));
  }

  @Test
  @WithMockUser(username = "testuser")
  void helloEndpoint_WithAuthentication_ShouldReturnHelloMessage() throws Exception {
    HelloResponse mockResponse =
        new HelloResponse("Hello, testuser!", "testuser", LocalDateTime.now(), "1.0.0");

    when(helloService.getHelloMessage(anyString())).thenReturn(mockResponse);

    mockMvc
        .perform(
            get("/api/hello").with(jwt().jwt(jwt -> jwt.claim("preferred_username", "testuser"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").exists())
        .andExpect(jsonPath("$.user").value("testuser"));
  }

  @Test
  void helloEndpoint_WithoutAuthentication_ShouldReturnLocalUser() throws Exception {
    HelloResponse mockResponse =
        new HelloResponse("Hello, local-user!", "local-user", LocalDateTime.now(), "1.0.0");

    when(helloService.getHelloMessage("local-user")).thenReturn(mockResponse);

    mockMvc
        .perform(get("/api/hello"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user").value("local-user"));
  }
}
