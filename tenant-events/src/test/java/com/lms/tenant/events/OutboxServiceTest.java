package com.lms.tenant.events;

import com.lms.tenant.events.core.DomainEvent;
import com.lms.tenant.events.publisher.OutboxService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxServiceTest {
  record TEvent(UUID id) implements DomainEvent {
    @Override public String eventType() { return "TenantCreated"; }
    @Override public UUID aggregateId() { return id; }
    @Override public String aggregateType() { return "tenant"; }
    @Override public Instant occurredAt() { return Instant.now(); }
  }

  @Test
  void append_serializes() {
    var ds = new DriverManagerDataSource("jdbc:h2:mem:test;MODE=PostgreSQL", "sa", "");
    var jdbc = new JdbcTemplate(ds);
    jdbc.execute("create table outbox_event(event_id uuid, event_type varchar, aggregate_type varchar, aggregate_id uuid, occurred_at timestamp, payload json, headers json, status varchar, attempts int, published_at timestamp)");
    var svc = new OutboxService(jdbc);
    UUID id = svc.append(new TEvent(UUID.randomUUID()), Map.of("x","y"));
    assertThat(id).isNotNull();
  }
}
