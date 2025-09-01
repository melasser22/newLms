package com.lms.tenant.events.publisher;

import com.lms.tenant.events.config.EventsProperties;
import com.lms.tenant.events.core.OutboxEvent;
import com.lms.tenant.events.core.OutboxStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Periodically reads from outbox using SKIP LOCKED and publishes. */
public class OutboxPublisher {
  private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
  private final OutboxService outboxService;
  private final EventsProperties props;
  private final KafkaPublisher kafka; // may be null
  private final LogPublisher logPublisher; // may be null
  private final NamedParameterJdbcTemplate jdbc;

  public OutboxPublisher(OutboxService outboxService, EventsProperties props, KafkaPublisher kafka, LogPublisher logPublisher) {
    this.outboxService = outboxService; this.props = props; this.kafka = kafka; this.logPublisher = logPublisher;
    this.jdbc = outboxService.getJdbc();
  }

  private List<OutboxEvent> lockBatch(int n) {
    String sql = "select * from outbox_event where status in ('NEW','FAILED') order by occurred_at for update skip locked limit :n";
    return jdbc.query(sql, new MapSqlParameterSource().addValue("n", n), (rs, i) -> {
      var e = new OutboxEvent();
      e.setEventId(java.util.UUID.fromString(rs.getString("event_id")));
      e.setEventType(rs.getString("event_type"));
      e.setAggregateType(rs.getString("aggregate_type"));
      e.setAggregateId(java.util.UUID.fromString(rs.getString("aggregate_id")));
      e.setOccurredAt(rs.getTimestamp("occurred_at").toInstant());
      e.setPayloadJson(rs.getString("payload"));
      e.setHeadersJson(rs.getString("headers"));
      e.setStatus(OutboxStatus.valueOf(rs.getString("status")));
      e.setAttempts(rs.getInt("attempts"));
      return e;
    });
  }

  @Scheduled(fixedDelayString = "${lms.events.schedule:PT1S}")
  @Transactional
  public void publish() {
    int batch = props.getBatchSize();
    List<OutboxEvent> events = lockBatch(batch);
    if (events.isEmpty()) return;

    for (OutboxEvent e : events) {
      try {
        if (kafka != null) kafka.publish(e); else if (logPublisher != null) logPublisher.publish(e);
        updateStatus(e, OutboxStatus.PUBLISHED, null);
      } catch (Exception ex) {
        int attempts = e.getAttempts() + 1;
        e.setAttempts(attempts);
        var newStatus = attempts >= props.getMaxAttempts() ? OutboxStatus.DEAD : OutboxStatus.FAILED;
        updateStatus(e, newStatus, ex);
      }
    }
  }

  private void updateStatus(OutboxEvent e, OutboxStatus status, Exception ex) {
    String sql = "update outbox_event set status=:s, attempts=:a, published_at = case when :s='PUBLISHED' then now() else published_at end where event_id=:id";
    jdbc.update(sql, new MapSqlParameterSource().addValue("s", status.name()).addValue("a", e.getAttempts()).addValue("id", e.getEventId()));
    if (ex != null) log.warn("Failed to publish event {} attempt {} => {}", e.getEventId(), e.getAttempts(), ex.toString());
  }
}
