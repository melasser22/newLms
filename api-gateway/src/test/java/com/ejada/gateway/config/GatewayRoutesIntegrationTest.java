package com.ejada.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "shared.ratelimit.enabled=false",
        "shared.security.mode=hs256",
        "shared.security.hs256.secret=" + GatewayRoutesIntegrationTest.SECRET,
        "spring.autoconfigure.exclude=com.ejada.shared_starter_ratelimit.RateLimitAutoConfiguration,com.ejada.kafka_starter.config.KafkaConsumerConfig,com.ejada.kafka_starter.config.KafkaProducerConfig",
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.bootstrap-servers=localhost:65535",
        "gateway.cache.kafka.enabled=false"
    }
)
@TestPropertySource(properties = {
    "gateway.routes.test.id=test-service",
    "gateway.routes.test.uri=http://localhost:65535",
    "gateway.routes.test.paths[0]=/api/test/**",
    "gateway.routes.test.resilience.enabled=true",
    "gateway.routes.test.resilience.fallback-status=BAD_GATEWAY",
    "gateway.routes.test.resilience.fallback-message=Custom outage message"
})
class GatewayRoutesIntegrationTest {

  static final String SECRET =
      "0123456789ABCDEF0123456789ABCDEF-SECURE-0123456789ABCDEF";

  @Autowired
  private WebTestClient webTestClient;

  @Autowired
  private RouteLocator routeLocator;

  @Test
  void fallbackEndpointHonoursConfiguredStatusAndMessage() {
    webTestClient
        .post()
        .uri("/fallback/test-service")
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.BAD_GATEWAY)
        .expectBody()
        .jsonPath("$.data.routeId")
        .isEqualTo("test-service")
        .jsonPath("$.message")
        .isEqualTo("Custom outage message");
  }

  @Test
  void routeLocatorRegistersConfiguredRoute() {
    assertThat(routeLocator.getRoutes().collectList().block(Duration.ofSeconds(5)))
        .anySatisfy(route -> {
          assertThat(route.getId()).isEqualTo("test-service");
          assertThat(route.getUri().toString()).isEqualTo("http://localhost:65535");
        });
  }

  @TestConfiguration
  static class AllowAllSecurityConfig {

    @Bean
    SecurityWebFilterChain testSecurityWebFilterChain(ServerHttpSecurity http) {
      return http.authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
          .csrf(ServerHttpSecurity.CsrfSpec::disable)
          .build();
    }
  }
}
