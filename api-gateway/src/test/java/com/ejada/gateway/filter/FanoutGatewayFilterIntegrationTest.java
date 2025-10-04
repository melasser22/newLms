package com.ejada.gateway.filter;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.client.VerificationException;
import org.junit.jupiter.api.AfterAll;
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
class FanoutGatewayFilterIntegrationTest {

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
    registry.add("shared.security.hs256.secret", () -> "fanout-test-secret-0123456789");
    registry.add("shared.security.resource-server.enabled", () -> "false");
    registry.add("shared.ratelimit.enabled", () -> "false");
    registry.add("gateway.subscription.enabled", () -> "false");

    String base = "http://localhost:" + WIREMOCK.port();
    registry.add("gateway.routes.primary.id", () -> "primary");
    registry.add("gateway.routes.primary.uri", () -> base + "/primary");
    registry.add("gateway.routes.primary.paths[0]", () -> "/api/primary");
    registry.add("gateway.routes.primary.strip-prefix", () -> "0");

    registry.add("gateway.fanout.enabled", () -> "true");
    registry.add("gateway.fanout.routes.primary.targets[0].id", () -> "audit");
    registry.add("gateway.fanout.routes.primary.targets[0].uri", () -> base + "/audit");
    registry.add("gateway.fanout.routes.primary.targets[0].method", () -> "POST");
    registry.add("gateway.fanout.routes.primary.targets[0].headers.X-Audit-Event", () -> "primary-change");
  }

  @Test
  void fanoutDoesNotBlockPrimaryResponse() throws Exception {
    stubFor(post(urlEqualTo("/primary"))
        .willReturn(aResponse().withStatus(202)));
    stubFor(post(urlEqualTo("/audit"))
        .willReturn(aResponse().withStatus(204)));

    webTestClient.post()
        .uri("/api/primary")
        .header("X-Tenant-Id", "tenant-1")
        .bodyValue("{\"event\":\"test\"}")
        .exchange()
        .expectStatus().isAccepted();

    // fan-out happens asynchronously, so poll for verification
    int attempts = 0;
    VerificationException last = null;
    while (attempts++ < 10) {
      try {
        verify(postRequestedFor(urlEqualTo("/audit")));
        last = null;
        break;
      } catch (VerificationException ex) {
        last = ex;
        Thread.sleep(100);
      }
    }
    if (last != null) {
      throw last;
    }

    verify(postRequestedFor(urlEqualTo("/primary")));
  }
}

