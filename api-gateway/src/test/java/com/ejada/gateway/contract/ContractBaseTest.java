package com.ejada.gateway.contract;

import com.ejada.gateway.config.TestGatewayConfiguration;
import com.ejada.gateway.config.RedisTestConfiguration;
import com.ejada.gateway.config.SubscriptionCacheTestConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Base class used by Spring Cloud Contract generated tests. It spins up the
 * gateway application on a random port so that contracts can be verified
 * against the real HTTP layer while still relying on lightweight test
 * doubles for JWT and circuit breaker behaviour.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {
    TestGatewayConfiguration.class,
    RedisTestConfiguration.class,
    SubscriptionCacheTestConfiguration.class
})
@TestPropertySource(properties = {
    "shared.security.resource-server.enabled=true",
    "chaos.monkey.enabled=false",
    "shared.ratelimit.enabled=false",
    "spring.autoconfigure.exclude=com.ejada.shared_starter_ratelimit.RateLimitAutoConfiguration,com.ejada.kafka_starter.config.KafkaConsumerConfig,com.ejada.kafka_starter.config.KafkaProducerConfig"
})
public abstract class ContractBaseTest {

  protected WebClient webClient;

  @BeforeEach
  void setUp() {
    this.webClient = WebClient.create();
  }

  @DynamicPropertySource
  static void contractProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.main.allow-bean-definition-overriding", () -> true);
    registry.add("spring.kafka.listener.auto-startup", () -> false);
    registry.add("spring.kafka.bootstrap-servers", () -> "localhost:65535");
  }
}
