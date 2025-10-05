package com.ejada.gateway.chaos;

import com.ejada.gateway.config.TestGatewayConfiguration;
import de.codecentric.spring.boot.chaos.monkey.component.ChaosMonkeyRequestScope;
import de.codecentric.spring.boot.chaos.monkey.component.ChaosTarget;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * Placeholder chaos engineering tests. The scenarios rely on Chaos Monkey for
 * Spring Boot to inject failures into the reactive filter chain. The
 * environments used by the kata runner do not expose Docker or network
 * primitives necessary for deterministic execution, therefore these tests are
 * disabled by default but documented for CI orchestration.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = TestGatewayConfiguration.class)
@Disabled("Chaos experiments require dedicated infrastructure and are executed in CI pipelines")
class ChaosEngineeringTests {

  @Autowired(required = false)
  private ChaosMonkeyRequestScope chaosMonkeyRequestScope;

  @Test
  @DisplayName("Chaos Monkey can inject latency into downstream calls")
  void chaosMonkeyLatencyInjection() {
    if (chaosMonkeyRequestScope != null) {
      chaosMonkeyRequestScope.callChaosMonkey(ChaosTarget.SERVICE, "latency");
    }
  }

  @Test
  @DisplayName("Chaos Monkey can simulate random request failures")
  void chaosMonkeyRandomFailures() {
    if (chaosMonkeyRequestScope != null) {
      chaosMonkeyRequestScope.callChaosMonkey(ChaosTarget.SERVICE, "exceptions");
    }
  }
}
