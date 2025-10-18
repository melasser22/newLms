package com.ejada.gateway.fallback;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ejada.gateway.config.GatewayRoutesProperties;
import com.ejada.gateway.config.GatewayRoutesProperties.ServiceRoute;
import com.ejada.gateway.config.ReactiveContextConfiguration;
import com.ejada.gateway.config.RedisTestConfiguration;
import com.ejada.gateway.config.SubscriptionCacheTestConfiguration;
import com.ejada.gateway.fallback.CachedFallbackService;
import com.ejada.gateway.fallback.BillingFallbackQueue;
import com.ejada.starter_core.config.CoreAutoConfiguration;
import com.ejada.gateway.resilience.TenantCircuitBreakerMetrics;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

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
@Import({GatewayFallbackControllerTest.TestSecurityConfig.class, RedisTestConfiguration.class,
    SubscriptionCacheTestConfiguration.class})
class GatewayFallbackControllerTest {

  @Autowired
  private WebTestClient webTestClient;

  @MockBean
  private BillingFallbackQueue billingFallbackQueue;

  @MockBean
  private CachedFallbackService cachedFallbackService;

  @MockBean
  private TenantCircuitBreakerMetrics tenantCircuitBreakerMetrics;

  @BeforeEach
  void setUpMocks() {
    when(billingFallbackQueue.enqueue(any(), any())).thenReturn(Mono.just("queue-key"));
    when(cachedFallbackService.resolve(any(), any())).thenReturn(Mono.empty());
  }

  @Test
  void fallbackReturnsServiceUnavailable() {
    webTestClient
        .post()
        .uri("/fallback/sample-route")
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
        .expectBody()
        .jsonPath("$.data.routeId")
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

    @Bean
    CoreAutoConfiguration.CoreProps coreProps() {
      CoreAutoConfiguration.CoreProps props = new CoreAutoConfiguration.CoreProps();
      props.getCorrelation().setEnabled(false);
      return props;
    }
  }
}
