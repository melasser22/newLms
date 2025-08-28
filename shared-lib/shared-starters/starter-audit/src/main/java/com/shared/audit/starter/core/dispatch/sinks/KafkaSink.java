package com.shared.audit.starter.core.dispatch.sinks;

import com.shared.audit.starter.api.AuditEvent;
import com.shared.audit.starter.util.JsonUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.Properties;
import java.util.concurrent.Future;

public class KafkaSink implements Sink {
  private final KafkaProducer<String, String> producer;
  private final String topic;

  public KafkaSink(String bootstrapServers, String topic, String acks, String compression, int timeoutMs) {
    Properties p = new Properties();
    p.put("bootstrap.servers", bootstrapServers);
    p.put("acks", acks);
    p.put("compression.type", compression);
    p.put("enable.idempotence", "true");
    p.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    p.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    this.producer = new KafkaProducer<>(p);
    this.topic = topic;
  }

  @Override public void send(AuditEvent event) throws Exception {
    String key = event.getTenantId() + ":" + event.getEventId();
    String value = JsonUtils.toJson(event);
    Future<RecordMetadata> f = producer.send(new ProducerRecord<>(topic, key, value));
    f.get(); // sync
  }
}
