package com.ejada.audit.starter.core.dispatch.support;

import com.ejada.audit.starter.api.AuditEvent;
import com.ejada.common.exception.JsonSerializationException;
import com.ejada.common.json.JsonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Locale;
import java.util.Set;

/** Utility for producing redacted JSON payloads for audit persistence/logging. */
public final class AuditEventJsonSerializer {

  private static final Set<String> SENSITIVE_KEYS = Set.of(
      "password",
      "accessToken",
      "authorization",
      "phoneNumber",
      "otp",
      "token"
  );

  private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = JsonUtils.mapper();

  private AuditEventJsonSerializer() {}

  public static String toRedactedJson(AuditEvent event) throws JsonSerializationException {
    if (event == null) {
      throw new IllegalArgumentException("event must not be null");
    }
    ObjectNode root = MAPPER.valueToTree(event);
    redact(root);
    try {
      return MAPPER.writeValueAsString(root);
    } catch (JsonProcessingException e) {
      throw new JsonSerializationException("Failed to serialize audit event", e);
    }
  }

  private static void redact(JsonNode node) {
    if (node == null) {
      return;
    }
    if (node.isObject()) {
      ObjectNode obj = (ObjectNode) node;
      obj.fieldNames().forEachRemaining(field -> {
        JsonNode child = obj.get(field);
        if (isSensitive(field)) {
          obj.put(field, "***");
        } else {
          redact(child);
        }
      });
      return;
    }
    if (node.isArray()) {
      ArrayNode array = (ArrayNode) node;
      for (JsonNode child : array) {
        redact(child);
      }
    }
  }

  private static boolean isSensitive(String fieldName) {
    return fieldName != null && SENSITIVE_KEYS.contains(fieldName.toLowerCase(Locale.ROOT));
  }
}
