package com.ejada.gateway.routes.service;

import com.ejada.gateway.routes.model.RouteCallAuditRecord;
import com.ejada.gateway.routes.repository.RouteCallAuditR2dbcRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

@SpringBootTest
@Testcontainers
@ExtendWith(SpringExtension.class)
class RouteCallAuditServiceIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("lms");

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.r2dbc.url", () ->
        String.format("r2dbc:postgresql://%s:%d/%s", POSTGRES.getHost(), POSTGRES.getFirstMappedPort(), POSTGRES.getDatabaseName()));
    registry.add("spring.r2dbc.username", POSTGRES::getUsername);
    registry.add("spring.r2dbc.password", POSTGRES::getPassword);
    registry.add("spring.flyway.enabled", () -> false);
    registry.add("spring.liquibase.enabled", () -> false);
    registry.add("spring.sql.init.mode", () -> "always");
    registry.add("spring.cloud.config.enabled", () -> false);
    registry.add("spring.cloud.kubernetes.enabled", () -> false);
  }

  @Autowired
  private RouteCallAuditService routeCallAuditService;

  @Autowired
  private RouteCallAuditR2dbcRepository repository;

  @AfterEach
  void clean() {
    StepVerifier.create(repository.deleteAll()).verifyComplete();
  }

  @Test
  void recordShouldPersistAuditEntry() {
    RouteCallAuditRecord record = new RouteCallAuditRecord(
        "44444444-4444-4444-4444-444444444444",
        "/api/auth/admins",
        "GET",
        401,
        25L,
        "tenant-1",
        "corr-123",
        "127.0.0.1",
        "ON_COMPLETE",
        null);

    StepVerifier.create(routeCallAuditService.record(record))
        .verifyComplete();

    StepVerifier.create(repository.count())
        .expectNext(1L)
        .verifyComplete();
  }
}
