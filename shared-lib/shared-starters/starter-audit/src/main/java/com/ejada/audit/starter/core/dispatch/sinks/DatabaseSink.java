// NEW VERSION – uses TransactionTemplate (REQUIRES_NEW)
package com.ejada.audit.starter.core.dispatch.sinks;

import com.ejada.audit.starter.api.AuditEvent;
import com.ejada.audit.starter.core.dispatch.support.AuditEventJsonSerializer;
import com.ejada.common.constants.HeaderNames;
import com.ejada.common.exception.JsonSerializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

public class DatabaseSink implements Sink {
  private static final Logger log = LoggerFactory.getLogger(DatabaseSink.class);

  private final JdbcTemplate jdbc;
  private final TransactionTemplate tx;
  private final String qualifiedTable;
  private final String insertSql;

  public DatabaseSink(JdbcTemplate jdbc, TransactionTemplate tx, String schema, String table) {
    this.jdbc = jdbc;
    this.tx = tx; // preconfigured with REQUIRES_NEW
    this.qualifiedTable = qualify(schema, table);
    this.insertSql =
        "INSERT INTO " + this.qualifiedTable + " (" +
        " id, ts_utc, x_tenant_id, actor_id, actor_username, action, entity_type, entity_id, outcome," +
        " data_class, sensitivity, resource_path, resource_method, correlation_id, span_id, message, payload) " +
        "VALUES (? , ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, cast(? as jsonb))";
  }

  @Override
  public void send(AuditEvent e) {
    try {
      // Pre-serialize outside of the transactional lambda to handle checked exceptions
      String payload;
      try {
        payload = AuditEventJsonSerializer.toRedactedJson(e);
      } catch (JsonSerializationException jsonEx) {
        log.warn("Failed to serialize audit event {} to JSON.", e.getEventId(), jsonEx);
        return;
      }
      tx.execute(status -> {
        int updated = jdbc.update(insertSql,
            e.getEventId(),
            java.sql.Timestamp.from(e.getTimestamp()),
            e.getTenantId(),
            e.getActor() == null ? null : e.getActor().id(),
            e.getActor() == null ? null : e.getActor().username(),
            e.getAction().name(),
            e.getEntityType(),
            e.getEntityId(),
            e.getOutcome().name(),
            e.getDataClass() == null ? null : e.getDataClass().name(),
            e.getSensitivity() == null ? null : e.getSensitivity().name(),
            e.getResource().getOrDefault("path", null),
            e.getResource().getOrDefault("method", null),
            e.getMetadata().getOrDefault(HeaderNames.CORRELATION_ID, null),
            null,
            e.getMessage(),
            payload
        );
        if (updated != 1) {
          log.warn("Audit insert affected {} rows for event {}", updated, e.getEventId());
        }
        return null;
      });
    } catch (Exception ex) {
      // Never break the request because of audit persistence
      log.warn("Failed to persist audit event {} to {}.", e.getEventId(), qualifiedTable, ex);
    }
  }

  private static String qualify(String schema, String table) {
    String effectiveSchema = (schema == null || schema.isBlank()) ? "public" : schema;
    return quote(effectiveSchema) + "." + quote(table);
  }

  private static String quote(String identifier) {
    if (identifier == null || identifier.isBlank()) {
      throw new IllegalArgumentException("Identifier must not be null or blank");
    }
    return "\"" + identifier.replace("\"", "\"\"") + "\"";
  }
}
