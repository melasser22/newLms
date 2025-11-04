package com.ejada.audit.starter.core.dispatch.sinks;

import com.ejada.audit.starter.api.AuditEvent;
import com.ejada.audit.starter.core.dispatch.support.AuditEventJsonSerializer;
import com.ejada.common.exception.JsonSerializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import com.ejada.common.constants.HeaderNames;
import java.util.UUID;

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
    this.insertSql =
        "INSERT INTO "
            + table
            + " (id, correlation_id, payload, status) VALUES (?, ?, cast(? as jsonb), 'NEW')";
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
    this.insertSql =
        "INSERT INTO "
            + table
            + " (id, correlation_id, payload, status) VALUES (?, ?, cast(? as jsonb), 'NEW')";
  }

  @Override
  public void send(AuditEvent event) {
    try {
      // Pre-serialize the payload. Handle checked serialization exceptions explicitly.
      String payload;
      try {
        payload = AuditEventJsonSerializer.toRedactedJson(event);
      } catch (JsonSerializationException jsonEx) {
        log.warn("Failed to serialize outbox event {} to JSON.", event.getEventId(), jsonEx);
        return;
      }
      String correlationId = null;
      Object corr = event.getMetadata().get(HeaderNames.CORRELATION_ID);
      if (corr != null) {
        correlationId = corr.toString();
      }
      UUID id = UUID.randomUUID();
      if (tx == null) {
        // legacy behavior (same TX as request)
        jdbc.update(insertSql, id, correlationId, payload);
      } else {
        // isolated TX so it commits even if the request rolls back
        final String cid = correlationId;
        final UUID rid = id;
        tx.execute(
            status -> {
              jdbc.update(insertSql, rid, cid, payload);
              return null;
            });
      }
    } catch (Exception ex) {
      // Never break the request because of outbox persistence
      log.warn("Failed to persist outbox event {} to {}.", event.getEventId(), table, ex);
    }
  }
}
