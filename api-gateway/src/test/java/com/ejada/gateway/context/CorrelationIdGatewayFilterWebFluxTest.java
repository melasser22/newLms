package com.ejada.gateway.context;

import static org.assertj.core.api.Assertions.assertThat;

import com.ejada.gateway.config.RedisTestConfiguration;
import com.ejada.gateway.config.SubscriptionCacheTestConfiguration;
import com.ejada.starter_core.config.CoreAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@WebFluxTest(
    controllers = CorrelationIdGatewayFilterWebFluxTest.DemoController.class,
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
            classes = com.ejada.gateway.ratelimit.ReactiveRateLimiterFilter.class)
    })
@Import({
    CorrelationIdGatewayFilter.class,
    CorrelationIdGatewayFilterWebFluxTest.TestConfig.class,
    CorrelationIdGatewayFilterWebFluxTest.SecurityConfig.class,
    CorrelationIdGatewayFilterWebFluxTest.DemoController.class,
    RedisTestConfiguration.class,
    SubscriptionCacheTestConfiguration.class
})
@TestPropertySource(properties = "shared.ratelimit.enabled=false")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class CorrelationIdGatewayFilterWebFluxTest {

  private final WebTestClient webTestClient;

  CorrelationIdGatewayFilterWebFluxTest(WebTestClient webTestClient) {
    this.webTestClient = webTestClient;
  }

  @Test
  void generatesCorrelationIdWhenMissing() {
    String correlationId = webTestClient.get()
        .uri("/demo")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk()
        .expectHeader().valueMatches("X-Correlation-Id", ".+")
        .expectBody(String.class)
        .returnResult()
        .getResponseHeaders()
        .getFirst("X-Correlation-Id");

    assertThat(correlationId).isNotBlank();
  }

  @Test
  void preservesExistingCorrelationId() {
    String correlationId = "corr-123";

    webTestClient.get()
        .uri("/demo")
        .header("X-Correlation-Id", correlationId)
        .exchange()
        .expectHeader().valueEquals("X-Correlation-Id", correlationId);
  }

  @RestController
  static class DemoController {

    @GetMapping("/demo")
    String demo() {
      return "ok";
    }
  }

  static class TestConfig {

    @Bean
    CoreAutoConfiguration.CoreProps coreProps() {
      CoreAutoConfiguration.CoreProps props = new CoreAutoConfiguration.CoreProps();
      props.getCorrelation().setEnabled(true);
      props.getCorrelation().setGenerateIfMissing(true);
      props.getCorrelation().setHeaderName("X-Correlation-Id");
      props.getCorrelation().setSkipPatterns(new String[0]);
      return props;
    }
  }

  static class SecurityConfig {

    @Bean
    SecurityWebFilterChain testSecurityWebFilterChain(ServerHttpSecurity http) {
      return http
          .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
          .csrf(ServerHttpSecurity.CsrfSpec::disable)
          .build();
    }
  }
}
