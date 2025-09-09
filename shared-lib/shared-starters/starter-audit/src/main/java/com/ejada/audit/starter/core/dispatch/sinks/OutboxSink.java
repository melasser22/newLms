package com.ejada.audit.starter.core.dispatch.sinks;

import com.ejada.audit.starter.api.AuditEvent;
import com.ejada.common.json.JsonUtils;
import com.ejada.common.exception.JsonSerializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

public class OutboxSink implements Sink {
  private static final Logger log = LoggerFactory.getLogger(OutboxSink.class);

  private final JdbcTemplate jdbc;
  private final TransactionTemplate tx; // may be null (if old ctor used)
  private final String table;
  private final String insertSql;

  // Old constructor (kept)
  public OutboxSink(JdbcTemplate jdbc, String table) {
    this.jdbc = jdbc;
    this.tx = null;
    this.table = table;
    this.insertSql = "INSERT INTO " + table + " (id, payload, status) VALUES (?, cast(? as jsonb), 'NEW')";
  }

  // New constructor (REQUIRES_NEW)
  public OutboxSink(JdbcTemplate jdbc, TransactionTemplate tx, String table) {
    this.jdbc = jdbc;
    this.tx = tx;
    // ensure REQUIRES_NEW even if template was not preconfigured
    if (this.tx != null && this.tx.getPropagationBehavior() != TransactionDefinition.PROPAGATION_REQUIRES_NEW) {
      this.tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }
    this.table = table;
    this.insertSql = "INSERT INTO " + table + " (id, payload, status) VALUES (?, cast(? as jsonb), 'NEW')";
  }

  @Override
  public void send(AuditEvent event) {
    try {
      // Pre-serialize the payload. Handle checked serialization exceptions explicitly.
      String payload;
      try {
        payload = JsonUtils.toJson(event);
      } catch (JsonSerializationException jsonEx) {
        log.warn("Failed to serialize outbox event {} to JSON.", event.getEventId(), jsonEx);
        return;
      }
      if (tx == null) {
        // legacy behavior (same TX as request)
        jdbc.update(insertSql, event.getEventId(), payload);
      } else {
        // isolated TX so it commits even if the request rolls back
        tx.execute(status -> {
          jdbc.update(insertSql, event.getEventId(), payload);
          return null;
        });
      }
    } catch (Exception ex) {
      // Never break the request because of outbox persistence
      log.warn("Failed to persist outbox event {} to {}.", event.getEventId(), table, ex);
    }
  }
}
