package com.lms.tenant.events.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.tenant.events.core.DomainEvent;
import com.lms.tenant.events.support.JsonSupport;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Minimal JDBC-based outbox writer; avoids JPA session constraints in calling services. */
public class OutboxService {
  private final NamedParameterJdbcTemplate jdbc;
  private final ObjectMapper om = JsonSupport.mapper();

  public OutboxService(JdbcTemplate jdbc) { this.jdbc = new NamedParameterJdbcTemplate(jdbc); }

  @Transactional
  public UUID append(DomainEvent evt, Map<String,Object> headers) {
    try {
      UUID id = UUID.randomUUID();
      String payload = om.writeValueAsString(evt);
      String headersJson = om.writeValueAsString(headers);
      var sql = """
        insert into outbox_event(event_id,event_type,aggregate_type,aggregate_id,occurred_at,payload,headers,status,attempts)
        values(:id,:etype,:atype,:aid,:ts, cast(:payload as jsonb), cast(:headers as jsonb),'NEW',0)
      """;
      var ps = new MapSqlParameterSource()
          .addValue("id", id)
          .addValue("etype", evt.eventType())
          .addValue("atype", evt.aggregateType())
          .addValue("aid", evt.aggregateId())
          .addValue("ts", evt.occurredAt()==null? Instant.now(): evt.occurredAt())
          .addValue("payload", payload)
          .addValue("headers", headersJson);
      jdbc.update(sql, ps);
      return id;
    } catch (Exception e) {
      throw new IllegalStateException("Failed to append to outbox", e);
    }
  }

  NamedParameterJdbcTemplate getJdbc() { return jdbc; }
}
