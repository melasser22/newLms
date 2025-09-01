package com.lms.tenant.events.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.tenant.events.config.EventsProperties;
import com.lms.tenant.events.core.OutboxEvent;
import com.lms.tenant.events.core.OutboxStatus;
import com.lms.tenant.events.support.JsonSupport;
import com.lms.tenant.events.support.TenantHeaderSupplier;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;

/** Publishes outbox events to Kafka using shared/spring Kafka template. */
public class KafkaPublisher {
  private final KafkaTemplate<String, String> template;
  private final EventsProperties props;
  private final TenantHeaderSupplier tenantHeaderSupplier;
  private final ObjectMapper om = JsonSupport.mapper();

  public KafkaPublisher(KafkaTemplate<String, String> template, EventsProperties props, TenantHeaderSupplier tenantHeaderSupplier) {
    this.template = template; this.props = props; this.tenantHeaderSupplier = tenantHeaderSupplier;
  }

  public void publish(OutboxEvent e) throws Exception {
    String topic = props.getTopicPrefix() + "." + e.getEventType();
    String key = e.getAggregateId().toString();
    var record = new ProducerRecord<String, String>(topic, key, e.getPayloadJson());
    // add headers
    tenantHeaderSupplier.headers().forEach((k,v) -> record.headers().add(k, String.valueOf(v).getBytes()));
    record.headers().add("event_id", e.getEventId().toString().getBytes());
    record.headers().add("occurred_at", e.getOccurredAt().toString().getBytes());

    template.send(record).get(); // block for simplicity; switch to callback if needed
    e.setStatus(OutboxStatus.PUBLISHED);
    e.setPublishedAt(Instant.now());
  }
}
