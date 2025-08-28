package com.shared.kafka_starter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

/**
 * Provides Jackson-based JSON (de)serialization for Kafka.
 * Ensures LocalDate/Instant work out-of-the-box and no type headers leak.
 */
public class KafkaJsonSerdeConfig {

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper kafkaObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule()); // support for java.time.*
        return mapper;
    }

    @Bean
    @ConditionalOnMissingBean
    public JsonSerializer<Object> jsonSerializer(ObjectMapper mapper) {
        JsonSerializer<Object> serializer = new JsonSerializer<>(mapper);
        // Avoid adding type headers, makes messages cross-language compatible
        serializer.setAddTypeInfo(false);
        return serializer;
    }

    @Bean
    @ConditionalOnMissingBean
    public JsonDeserializer<Object> jsonDeserializer(ObjectMapper mapper) {
        JsonDeserializer<Object> deserializer = new JsonDeserializer<>(mapper);
        deserializer.addTrustedPackages("*"); // allow all packages by default
        deserializer.ignoreTypeHeaders();     // ignore __TypeId__ headers
        return deserializer;
    }

    @Bean
    @ConditionalOnMissingBean
    public StringSerializer stringSerializer() {
        return new StringSerializer();
    }

    @Bean
    @ConditionalOnMissingBean
    public StringDeserializer stringDeserializer() {
        return new StringDeserializer();
    }
}
