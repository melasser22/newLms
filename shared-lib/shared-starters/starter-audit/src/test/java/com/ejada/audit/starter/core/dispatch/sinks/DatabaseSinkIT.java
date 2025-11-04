package com.ejada.audit.starter.core.dispatch.sinks;

import static org.assertj.core.api.Assertions.assertThat;

import com.ejada.audit.starter.api.AuditAction;
import com.ejada.audit.starter.api.AuditEvent;
import com.ejada.audit.starter.api.AuditOutcome;
import com.ejada.audit.starter.api.DataClass;
import com.ejada.audit.starter.api.Sensitivity;
import com.ejada.audit.starter.api.context.Actor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.PostgreSQLContainer;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension.class)
class DatabaseSinkIT {

  private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("audit")
          .withUsername("audit")
          .withPassword("audit");

  private JdbcTemplate jdbcTemplate;
  private TransactionTemplate transactionTemplate;

  @BeforeAll
  void setUp() {
    DriverManagerDataSource dataSource = new DriverManagerDataSource();
    dataSource.setDriverClassName("org.postgresql.Driver");
    dataSource.setUrl(POSTGRES.getJdbcUrl());
    dataSource.setUsername(POSTGRES.getUsername());
    dataSource.setPassword(POSTGRES.getPassword());

    this.jdbcTemplate = new JdbcTemplate(dataSource);
    PlatformTransactionManager txManager = new org.springframework.jdbc.datasource.DataSourceTransactionManager(dataSource);
    this.transactionTemplate = new TransactionTemplate(txManager);
    this.transactionTemplate.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);

    jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS \"security-service\"");
    jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS \"security-service\".audit_logs (" +
        "id UUID PRIMARY KEY, " +
        "ts_utc TIMESTAMPTZ NOT NULL, " +
        "x_tenant_id VARCHAR NULL, " +
        "actor_id VARCHAR NULL, " +
        "actor_username VARCHAR NULL, " +
        "action VARCHAR NOT NULL, " +
        "entity_type VARCHAR NULL, " +
        "entity_id VARCHAR NULL, " +
        "outcome VARCHAR NOT NULL, " +
        "data_class VARCHAR NOT NULL, " +
        "sensitivity VARCHAR NOT NULL, " +
        "resource_path VARCHAR NULL, " +
        "resource_method VARCHAR NULL, " +
        "correlation_id VARCHAR NULL, " +
        "span_id VARCHAR NULL, " +
        "message TEXT NULL, " +
        "payload JSONB NOT NULL" +
        ")");
  }

  @Test
  void writesAuditEventWhenSchemaContainsHyphenAndRedactsSensitiveValues() throws Exception {
    DatabaseSink sink = new DatabaseSink(jdbcTemplate, transactionTemplate, "security-service", "audit_logs");

    AuditEvent event = AuditEvent.builder()
        .tenantId("tenant")
        .actor(new Actor("123", "alice", java.util.List.of("ADMIN"), "BASIC"))
        .action(AuditAction.LOGIN)
        .outcome(AuditOutcome.SUCCESS)
        .sensitivity(Sensitivity.INTERNAL)
        .dataClass(DataClass.PII)
        .timestamp(Instant.now())
        .resource("path", "/api/v1/auth/admin/login")
        .resource("method", "POST")
        .meta(com.ejada.common.constants.HeaderNames.CORRELATION_ID, "corr-1")
        .meta("authorization", "Bearer secret")
        .put("password", "super-secret")
        .put("details", Map.of("otp", "000000", "safe", "value"))
        .message("login ok")
        .build();

    sink.send(event);

    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM \"security-service\".audit_logs WHERE id = ?",
        Integer.class,
        event.getEventId());

    assertThat(count).isEqualTo(1);

    String payloadJson = jdbcTemplate.queryForObject(
        "SELECT payload::text FROM \"security-service\".audit_logs WHERE id = ?",
        String.class,
        event.getEventId());

    JsonNode payloadNode = MAPPER.readTree(payloadJson);
    assertThat(payloadNode.at("/payload/password").asText()).isEqualTo("***");
    assertThat(payloadNode.at("/payload/details/otp").asText()).isEqualTo("***");
    assertThat(payloadNode.at("/payload/details/safe").asText()).isEqualTo("value");
    assertThat(payloadNode.at("/metadata/authorization").asText()).isEqualTo("***");
  }
}
