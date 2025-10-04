package com.ejada.gateway.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import com.ejada.common.constants.HeaderNames;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "shared.ratelimit.enabled=false",
        "shared.security.mode=hs256",
        "shared.security.hs256.secret=0123456789ABCDEF0123456789ABCDEF-SECRET-0123456789ABCDEF",
        "gateway.logging.access-log.enabled=false"
    })
@TestPropertySource(properties = {
    "management.endpoints.web.exposure.include=health,info",
    "gateway.tracing.enhanced-tags.enabled=true"
})
class GatewayMetricsIntegrationTest {

  @Autowired
  private WebTestClient webTestClient;

  @Autowired
  private MeterRegistry meterRegistry;

  @Test
  void recordsRequestMetricsPerTenant() {
    webTestClient.get()
        .uri("/test/metrics")
        .accept(MediaType.APPLICATION_JSON)
        .header(HeaderNames.X_TENANT_ID, "tenant-integration")
        .exchange()
        .expectStatus()
        .isOk();

    double count = meterRegistry.counter("gateway.requests.by_tenant",
            "tenantId", "tenant-integration",
            "statusCode", "200")
        .count();
    assertThat(count).isEqualTo(1.0d);

    Timer timer = meterRegistry.find("gateway.route.latency")
        .tags("routeId", "unknown")
        .timer();
    assertThat(timer).isNotNull();
    assertThat(timer.count()).isEqualTo(1L);
  }

  @TestConfiguration
  static class TestEndpoints {

    @Bean
    SecurityWebFilterChain allowAllSecurity(ServerHttpSecurity http) {
      return http.authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
          .csrf(ServerHttpSecurity.CsrfSpec::disable)
          .build();
    }

    @RestController
    static class ProbeController {

      @GetMapping("/test/metrics")
      Mono<String> ok() {
        return Mono.just("ok");
      }
    }
  }
}
