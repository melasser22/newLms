package com.shared.kafka_starter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.common.constants.HeaderNames;
import com.common.context.ContextManager;
import com.shared.kafka_starter.props.KafkaProperties;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Map;

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
    pf.addPostProcessor(rec -> {
      String cid = ContextManager.getCorrelationId();
      if (cid != null) {
        rec.headers().add(HeaderNames.CORRELATION_ID, cid.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      }
      return rec;
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
