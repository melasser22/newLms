package com.ejada.kafka_starter.config;

import com.ejada.common.constants.HeaderNames;
import com.ejada.common.context.ContextManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ejada.kafka_starter.props.KafkaProperties;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@AutoConfiguration
public class KafkaConsumerConfig {

    @Bean
    @ConditionalOnMissingBean
    public ConsumerFactory<String, Object> consumerFactory(KafkaProperties props, ObjectMapper om) {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, props.getBootstrapServers());
        cfg.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        cfg.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        cfg.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        cfg.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        cfg.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        cfg.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        cfg.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, props.getAutoOffsetReset());
        cfg.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, props.getMaxPollRecords());
        cfg.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, (int) props.getSessionTimeout().toMillis());
        cfg.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, (int) props.getHeartbeatInterval().toMillis());
        if (props.getGroupId() != null) {
            cfg.put(ConsumerConfig.GROUP_ID_CONFIG, props.getGroupId());
        }
        return new DefaultKafkaConsumerFactory<>(cfg);
    }

    @Bean
    @ConditionalOnMissingBean(name = "kafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> cf,
            KafkaTemplate<String, Object> template,
            KafkaProperties props) {

        var factory = new ConcurrentKafkaListenerContainerFactory<String, Object>();
        factory.setConsumerFactory(cf);
        factory.setConcurrency(props.getConcurrency());

        // --- Retry + DLT ---
        var backoff = new ExponentialBackOffWithMaxRetries(props.getMaxAttempts() - 1);
        backoff.setInitialInterval(props.getBackoff().toMillis());
        backoff.setMultiplier(2.0);
        backoff.setMaxInterval(10_000);

        // IMPORTANT: must return TopicPartition, not String
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                template,
                (record, ex) -> new TopicPartition(record.topic() + ".dlt", record.partition())
        );

        var errorHandler = new DefaultErrorHandler(recoverer, backoff);
        factory.setCommonErrorHandler(errorHandler);

        factory.setRecordInterceptor((record, consumer) -> {
            Headers headers = record.headers();
            var header = headers.lastHeader(HeaderNames.CORRELATION_ID);
            if (header != null) {
                String cid = new String(header.value(), java.nio.charset.StandardCharsets.UTF_8);
                ContextManager.setCorrelationId(cid);
                org.slf4j.MDC.put(HeaderNames.CORRELATION_ID, cid);
            }
            return record;
        });

        return factory;
    }
}
