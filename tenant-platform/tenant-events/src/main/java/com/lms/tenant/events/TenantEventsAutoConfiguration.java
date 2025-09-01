package com.lms.tenant.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

@AutoConfiguration
@EnableConfigurationProperties(TenantEventsProperties.class)
@EnableScheduling
@ConditionalOnProperty(prefix = "tenant.events", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TenantEventsAutoConfiguration {

    @Bean
    public TenantEventWriter tenantEventWriter(JdbcTemplate jdbcTemplate, ObjectMapper mapper) {
        return new TenantEventWriter(jdbcTemplate, mapper);
    }

    @Bean
    public TenantEventPublisher tenantEventPublisher(JdbcTemplate jdbcTemplate,
            ObjectProvider<KafkaTemplate<String, String>> kafkaTemplate,
            TenantEventsProperties properties) {
        return new TenantEventPublisher(jdbcTemplate, kafkaTemplate, properties);
    }
}
