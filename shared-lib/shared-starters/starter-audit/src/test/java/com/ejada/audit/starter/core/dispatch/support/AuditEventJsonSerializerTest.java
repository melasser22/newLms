package com.ejada.audit.starter.core.dispatch.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.ejada.audit.starter.api.AuditAction;
import com.ejada.audit.starter.api.AuditEvent;
import com.ejada.audit.starter.api.AuditOutcome;
import com.ejada.audit.starter.api.DataClass;
import com.ejada.audit.starter.api.Sensitivity;
import com.ejada.audit.starter.api.context.Actor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class AuditEventJsonSerializerTest {

  private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

  @Test
  void redactsSensitiveFieldsAcrossPayloads() throws Exception {
    AuditEvent event = AuditEvent.builder()
        .tenantId("tenant")
        .actor(new Actor("42", "alice", java.util.List.of("ADMIN"), "BASIC"))
        .action(AuditAction.LOGIN)
        .outcome(AuditOutcome.SUCCESS)
        .sensitivity(Sensitivity.INTERNAL)
        .dataClass(DataClass.PII)
        .put("password", "s3cr3t")
        .put("nested", java.util.Map.of("token", "abc", "safe", "value"))
        .meta("authorization", "Bearer token")
        .message("Login successful")
        .build();

    String json = AuditEventJsonSerializer.toRedactedJson(event);
    JsonNode root = MAPPER.readTree(json);

    assertThat(root.at("/payload/password").asText()).isEqualTo("***");
    assertThat(root.at("/payload/nested/token").asText()).isEqualTo("***");
    assertThat(root.at("/payload/nested/safe").asText()).isEqualTo("value");
    assertThat(root.at("/metadata/authorization").asText()).isEqualTo("***");
  }

  @Test
  void rejectsNullEvents() {
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> AuditEventJsonSerializer.toRedactedJson(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("event must not be null");
  }
}
