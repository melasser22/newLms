package com.ejada.kafka_starter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ejada.common.constants.HeaderNames;
import com.ejada.common.context.ContextManager;
import com.ejada.kafka_starter.props.KafkaProperties;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.apache.kafka.clients.consumer.ConsumerGroupMetadata;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.Uuid;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;

public class KafkaProducerConfig {

  @Bean
  @ConditionalOnMissingBean
  public ProducerFactory<String, Object> producerFactory(KafkaProperties props, ObjectMapper om) {
    var cfg = Map.<String, Object>of(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, props.getBootstrapServers(),
        ProducerConfig.ACKS_CONFIG, "all",
        ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, props.isExactlyOnce(),
        ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1,
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class,
        JsonSerializer.ADD_TYPE_INFO_HEADERS, false
    );
    DefaultKafkaProducerFactory<String,Object> pf = new DefaultKafkaProducerFactory<>(cfg);
    pf.addPostProcessor(producer -> new Producer<>() {
      private void addCorrelation(ProducerRecord<String, Object> rec) {
        String cid = ContextManager.getCorrelationId();
        if (cid != null) {
          rec.headers().add(HeaderNames.CORRELATION_ID, cid.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
      }

      @Override
      public Future<RecordMetadata> send(ProducerRecord<String, Object> rec) {
        addCorrelation(rec);
        return producer.send(rec);
      }

      @Override
      public Future<RecordMetadata> send(ProducerRecord<String, Object> rec, Callback callback) {
        addCorrelation(rec);
        return producer.send(rec, callback);
      }

      @Override
      public void flush() {
        producer.flush();
      }

      @Override
      public List<PartitionInfo> partitionsFor(String topic) {
        return producer.partitionsFor(topic);
      }

      @Override
      public Map<MetricName, ? extends Metric> metrics() {
        return producer.metrics();
      }

      @Override
      public void close() {
        producer.close();
      }

      @Override
      public void close(Duration timeout) {
        producer.close(timeout);
      }

      @Override
      public Uuid clientInstanceId(Duration timeout) {
        return producer.clientInstanceId(timeout);
      }

      @Override
      public void initTransactions() {
        producer.initTransactions();
      }

      @Override
      public void beginTransaction() {
        producer.beginTransaction();
      }

      @Override
      public void sendOffsetsToTransaction(Map<TopicPartition, OffsetAndMetadata> offsets, String groupId) {
        producer.sendOffsetsToTransaction(offsets, groupId);
      }

      @Override
      public void sendOffsetsToTransaction(Map<TopicPartition, OffsetAndMetadata> offsets, ConsumerGroupMetadata groupMetadata) {
        producer.sendOffsetsToTransaction(offsets, groupMetadata);
      }

      @Override
      public void commitTransaction() {
        producer.commitTransaction();
      }

      @Override
      public void abortTransaction() {
        producer.abortTransaction();
      }
    });
    if (props.isExactlyOnce()) {
      pf.setTransactionIdPrefix(props.getTxIdPrefix());
    }
    return pf;
  }

  @Bean
  @ConditionalOnMissingBean
  public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String,Object> pf) {
    return new KafkaTemplate<>(pf);
  }
}
