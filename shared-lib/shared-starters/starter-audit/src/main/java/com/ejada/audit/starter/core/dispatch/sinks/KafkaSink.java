package com.ejada.audit.starter.core.dispatch.sinks;

import com.ejada.audit.starter.api.AuditEvent;
import com.ejada.common.json.JsonUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Future;

public class KafkaSink implements Sink {
  private final KafkaProducer<String, String> producer;
  private final String topic;

  public KafkaSink(String bootstrapServers, String topic, String acks, String compression, int timeoutMs) {
    this(bootstrapServers, topic, buildOverrides(acks, compression, timeoutMs));
  }

  public KafkaSink(String bootstrapServers, String topic, Map<String, ?> overrides) {
    this(new KafkaProducer<>(buildProperties(bootstrapServers, overrides)), topic);
  }

  public KafkaSink(String bootstrapServers, String topic) {
    this(bootstrapServers, topic, Map.of());
  }

  private KafkaSink(KafkaProducer<String, String> producer, String topic) {
    this.producer = producer;
    this.topic = topic;
  }

  private static Properties buildProperties(String bootstrapServers, Map<String, ?> overrides) {
    Properties properties = new Properties();
    properties.put("bootstrap.servers", bootstrapServers);
    properties.put("acks", "all");
    properties.put("compression.type", "zstd");
    properties.put("enable.idempotence", "true");
    properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

    if (overrides != null) {
      overrides.forEach((key, value) -> {
        if (key != null && value != null) {
          properties.put(key, String.valueOf(value));
        }
      });
    }

    return properties;
  }

  private static Map<String, Object> buildOverrides(String acks, String compression, int timeoutMs) {
    Map<String, Object> overrides = new HashMap<>();
    if (acks != null && !acks.isBlank()) {
      overrides.put("acks", acks);
    }
    if (compression != null && !compression.isBlank()) {
      overrides.put("compression.type", compression);
    }
    if (timeoutMs > 0) {
      overrides.put("delivery.timeout.ms", timeoutMs);
    }
    return overrides;
  }

  @Override public void send(AuditEvent event) throws Exception {
    String key = event.getTenantId() + ":" + event.getEventId();
    String value = JsonUtils.toJson(event);
    Future<RecordMetadata> f = producer.send(new ProducerRecord<>(topic, key, value));
    f.get(); // sync
  }
}
