package com.lms.tenant.events.publisher;

import com.lms.tenant.events.config.EventsProperties;
import com.lms.tenant.events.core.OutboxEvent;
import com.lms.tenant.events.core.OutboxStatus;
import com.lms.tenant.events.support.TenantHeaderSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/** Fallback publisher if Kafka is not on the classpath. */
public class LogPublisher {
  private static final Logger log = LoggerFactory.getLogger(LogPublisher.class);
  private final EventsProperties props;
  private final TenantHeaderSupplier tenantHeaderSupplier;

  public LogPublisher(EventsProperties props, TenantHeaderSupplier tenantHeaderSupplier) {
    this.props = props; this.tenantHeaderSupplier = tenantHeaderSupplier;
  }

  public void publish(OutboxEvent e) {
    log.info("[OUTBOX->LOG] topic={} key={} eventId={} type={} payload={} headers={}",
        props.getTopicPrefix() + "." + e.getEventType(), e.getAggregateId(), e.getEventId(), e.getEventType(),
        e.getPayloadJson(), tenantHeaderSupplier.headers());
    e.setStatus(OutboxStatus.PUBLISHED);
    e.setPublishedAt(Instant.now());
  }
}
