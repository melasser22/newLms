package com.ejada.tenant.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ejada.tenant.events.entity.TenantOutboxEvent;
import com.ejada.tenant.events.repo.TenantOutboxEventRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

@AutoConfiguration
@EnableConfigurationProperties(TenantEventsProperties.class)
@EnableScheduling
@EntityScan(basePackageClasses = TenantOutboxEvent.class)
@EnableJpaRepositories(basePackageClasses = TenantOutboxEventRepository.class)
@ConditionalOnProperty(prefix = "tenant.events", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TenantEventsAutoConfiguration {

    @Bean
    public TenantEventWriter tenantEventWriter(TenantOutboxEventRepository repository, ObjectMapper mapper) {
        return new TenantEventWriter(repository, mapper);
    }

    @Bean
    public TenantEventPublisher tenantEventPublisher(TenantOutboxEventRepository repository,
            ObjectProvider<KafkaTemplate<String, String>> kafkaTemplate,
            TenantEventsProperties properties) {
        return new TenantEventPublisher(repository, kafkaTemplate, properties);
    }
}
