// NEW VERSION – uses TransactionTemplate (REQUIRES_NEW)
package com.shared.audit.starter.core.dispatch.sinks;

import com.shared.audit.starter.api.AuditEvent;
import com.shared.audit.starter.util.JsonUtils;
import com.common.exception.JsonSerializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

public class DatabaseSink implements Sink {
  private static final Logger log = LoggerFactory.getLogger(DatabaseSink.class);

  private final JdbcTemplate jdbc;
  private final TransactionTemplate tx;
  private final String table;
  private final String insertSql;

  public DatabaseSink(JdbcTemplate jdbc, TransactionTemplate tx, String schema, String table) {
    this.jdbc = jdbc;
    this.tx = tx; // preconfigured with REQUIRES_NEW
    this.table = (schema == null || schema.isBlank() ? "public" : schema) + "." + table;
    this.insertSql =
        "INSERT INTO " + this.table + " (" +
        " id, ts_utc, tenant_id, actor_id, actor_username, action, entity_type, entity_id, outcome," +
        " data_class, sensitivity, resource_path, resource_method, correlation_id, span_id, message, payload) " +
        "VALUES (? , ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, cast(? as jsonb))";
  }

  @Override
  public void send(AuditEvent e) {
    try {
      // Pre-serialize outside of the transactional lambda to handle checked exceptions
      String payload;
      try {
        payload = JsonUtils.toJson(e);
      } catch (JsonSerializationException jsonEx) {
        // Don’t propagate; log and abort persisting this event
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
            e.getMetadata().getOrDefault("correlationId", null),
            e.getMetadata().getOrDefault("spanId", null),
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
      log.warn("Failed to persist audit event {} to {}.", e.getEventId(), table, ex);
    }
  }
}
