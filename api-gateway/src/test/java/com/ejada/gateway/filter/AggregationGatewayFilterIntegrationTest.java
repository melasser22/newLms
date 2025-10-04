package com.ejada.gateway.filter;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AggregationGatewayFilterIntegrationTest {

  private static final WireMockServer WIREMOCK = new WireMockServer(WireMockConfiguration.options().dynamicPort());

  @Autowired
  private WebTestClient webTestClient;

  @AfterAll
  void stopWireMock() {
    WIREMOCK.stop();
  }

  @DynamicPropertySource
  static void gatewayProperties(DynamicPropertyRegistry registry) {
    if (!WIREMOCK.isRunning()) {
      WIREMOCK.start();
    }
    registry.add("shared.security.mode", () -> "hs256");
    registry.add("shared.security.hs256.secret", () -> "aggregation-test-secret-0123456789");
    registry.add("shared.security.resource-server.enabled", () -> "false");
    registry.add("shared.ratelimit.enabled", () -> "false");
    registry.add("gateway.subscription.enabled", () -> "false");

    String base = "http://localhost:" + WIREMOCK.port();
    registry.add("gateway.routes.dashboard.id", () -> "dashboard");
    registry.add("gateway.routes.dashboard.uri", () -> "no://op");
    registry.add("gateway.routes.dashboard.paths[0]", () -> "/api/dashboard");
    registry.add("gateway.routes.dashboard.strip-prefix", () -> "0");

    registry.add("gateway.aggregation.enabled", () -> "true");
    registry.add("gateway.aggregation.routes.dashboard.timeout", () -> "PT4S");
    registry.add("gateway.aggregation.routes.dashboard.upstream-requests[0].id", () -> "profile");
    registry.add("gateway.aggregation.routes.dashboard.upstream-requests[0].uri", () -> base + "/tenant/{tenantId}");
    registry.add("gateway.aggregation.routes.dashboard.upstream-requests[0].circuit-breaker-name", () -> "agg-profile");
    registry.add("gateway.aggregation.routes.dashboard.upstream-requests[1].id", () -> "subscriptions");
    registry.add("gateway.aggregation.routes.dashboard.upstream-requests[1].uri", () -> base + "/subscriptions?tenantId={tenantId}");
    registry.add("gateway.aggregation.routes.dashboard.upstream-requests[1].timeout", () -> "PT3S");
    registry.add("gateway.aggregation.routes.dashboard.upstream-requests[1].circuit-breaker-name", () -> "agg-subscriptions");
    registry.add("gateway.aggregation.routes.dashboard.upstream-requests[2].id", () -> "usage");
    registry.add("gateway.aggregation.routes.dashboard.upstream-requests[2].uri", () -> base + "/usage/{tenantId}");
    registry.add("gateway.aggregation.routes.dashboard.upstream-requests[2].optional", () -> "true");
  }

  @Test
  void aggregatesDownstreamResponsesAndCapturesFailures() {
    stubFor(get(urlEqualTo("/tenant/tenant-1"))
        .willReturn(okJson("{\"id\":\"tenant-1\",\"name\":\"Tenant One\"}")));
    stubFor(get(urlEqualTo("/subscriptions?tenantId=tenant-1"))
        .willReturn(okJson("[{\"id\":\"sub-1\"}]")));
    stubFor(get(urlEqualTo("/usage/tenant-1"))
        .willReturn(aResponse().withStatus(500)));

    webTestClient.get()
        .uri("/api/dashboard")
        .header("X-Tenant-Id", "tenant-1")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.data.profile.id").isEqualTo("tenant-1")
        .jsonPath("$.data.subscriptions[0].id").isEqualTo("sub-1")
        .jsonPath("$.errors.usage").isEqualTo("Unavailable");
  }
}

