package com.ejada.gateway.integration;

import com.ejada.gateway.config.TestGatewayConfiguration;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = TestGatewayConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
public abstract class GatewayIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

  @Container
  static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

  protected static WireMockServer wireMockServer;

  @LocalServerPort
  protected int port;

  @Autowired
  protected WebTestClient webTestClient;

  @BeforeAll
  @SuppressWarnings("resource")
  static void startWireMock() {
    wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
    wireMockServer.start();
  }

  @AfterAll
  static void stopWireMock() {
    if (wireMockServer != null) {
      wireMockServer.stop();
    }
  }

  @BeforeEach
  void resetWireMock() {
    wireMockServer.resetAll();
  }

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.r2dbc.url", () -> String.format("r2dbc:postgresql://%s:%d/testdb",
        POSTGRES.getHost(), POSTGRES.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)));
    registry.add("spring.r2dbc.username", POSTGRES::getUsername);
    registry.add("spring.r2dbc.password", POSTGRES::getPassword);
    registry.add("spring.data.redis.host", REDIS::getHost);
    registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    registry.add("gateway.bff.dashboard.tenant-service-uri", () -> wireMockServer.baseUrl());
    registry.add("gateway.bff.dashboard.analytics-service-uri", () -> wireMockServer.baseUrl());
    registry.add("gateway.bff.dashboard.billing-service-uri", () -> wireMockServer.baseUrl());
    registry.add("shared.security.resource-server.enabled", () -> true);
    registry.add("chaos.monkey.enabled", () -> false);
    registry.add("spring.main.allow-bean-definition-overriding", () -> true);
  }

  protected String gatewayUrl(String path) {
    return "http://localhost:" + port + path;
  }

  protected void stubGet(String url, int status, String body) {
    wireMockServer.stubFor(WireMock.get(WireMock.urlEqualTo(url))
        .willReturn(WireMock.aResponse().withStatus(status).withBody(body)
            .withHeader("Content-Type", "application/json")));
  }
}
