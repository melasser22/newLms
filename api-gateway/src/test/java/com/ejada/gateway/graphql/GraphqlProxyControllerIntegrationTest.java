package com.ejada.gateway.graphql;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.util.Map;
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
class GraphqlProxyControllerIntegrationTest {

  private static final WireMockServer WIREMOCK = new WireMockServer(WireMockConfiguration.options().dynamicPort());

  @Autowired
  private WebTestClient webTestClient;

  @AfterAll
  void stopWireMock() {
    WIREMOCK.stop();
  }

  @DynamicPropertySource
  static void graphqlProperties(DynamicPropertyRegistry registry) {
    if (!WIREMOCK.isRunning()) {
      WIREMOCK.start();
    }
    registry.add("shared.security.mode", () -> "hs256");
    registry.add("shared.security.hs256.secret", () -> "graphql-test-secret-0123456789");
    registry.add("shared.security.resource-server.enabled", () -> "false");
    registry.add("shared.ratelimit.enabled", () -> "false");
    registry.add("gateway.subscription.enabled", () -> "false");

    String base = "http://localhost:" + WIREMOCK.port();
    registry.add("gateway.graphql.enabled", () -> "true");
    registry.add("gateway.graphql.upstream-uri", () -> base + "/graphql");
    registry.add("gateway.graphql.timeout", () -> "PT3S");
    registry.add("gateway.graphql.max-depth", () -> "8");
    registry.add("gateway.graphql.max-breadth", () -> "20");
    registry.add("gateway.graphql.max-complexity", () -> "3");
  }

  @Test
  void proxiesGraphqlRequestsWithAnalysis() {
    stubFor(post(urlEqualTo("/graphql"))
        .willReturn(aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody("{\"data\":{\"me\":{\"id\":\"123\"}}}")));

    webTestClient.post()
        .uri("/graphql")
        .bodyValue(Map.of("query", "query { me { id } }"))
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.data.me.id").isEqualTo("123");

    verify(postRequestedFor(urlEqualTo("/graphql")));
  }

  @Test
  void rejectsQueriesExceedingComplexity() {
    webTestClient.post()
        .uri("/graphql")
        .bodyValue(Map.of("query", "query { a { b { c { d } } } }"))
        .exchange()
        .expectStatus().isBadRequest();
  }
}

