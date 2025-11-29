// Main application test
package com.example.aks;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AksStarterApplicationTests {

  @Test
  void contextLoads() {
    // Test that Spring context loads successfully
  }
}
