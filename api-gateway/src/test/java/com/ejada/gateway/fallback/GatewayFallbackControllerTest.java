package com.ejada.gateway.fallback;

import com.ejada.gateway.config.GatewayRoutesProperties;
import com.ejada.gateway.config.GatewayRoutesProperties.ServiceRoute;
import com.ejada.gateway.config.ReactiveContextConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(
    controllers = GatewayFallbackController.class,
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = ReactiveContextConfiguration.class),
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = com.ejada.gateway.ratelimit.ReactiveRateLimiterFilter.class)
    })
@TestPropertySource(properties = "shared.ratelimit.enabled=false")
@Import(GatewayFallbackControllerTest.TestSecurityConfig.class)
class GatewayFallbackControllerTest {

  @Autowired
  private WebTestClient webTestClient;

  @Test
  void fallbackReturnsServiceUnavailable() {
    webTestClient
        .post()
        .uri("/fallback/sample-route")
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
        .expectBody()
        .jsonPath("$.routeId")
        .isEqualTo("sample-route")
        .jsonPath("$.message")
        .isNotEmpty();
  }

  @TestConfiguration
  static class TestSecurityConfig {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
      return http
          .authorizeExchange(spec -> spec.anyExchange().permitAll())
          .csrf(ServerHttpSecurity.CsrfSpec::disable)
          .build();
    }

    @Bean
    GatewayRoutesProperties gatewayRoutesProperties() {
      GatewayRoutesProperties properties = new GatewayRoutesProperties();

      ServiceRoute route = new ServiceRoute();
      route.setId("sample-route");
      route.setUri("http://example.org");
      route.setPaths(List.of("/fallback/sample-route"));

      properties.getRoutes().put(route.getId(), route);
      return properties;
    }
  }
}
