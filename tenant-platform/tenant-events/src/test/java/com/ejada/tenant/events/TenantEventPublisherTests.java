package com.ejada.tenant.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.ejada.tenant.events.entity.TenantOutboxEvent;
import com.ejada.tenant.events.repo.TenantOutboxEventRepository;
import com.ejada.tenant.events.OutboxPublisher;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = TenantEventPublisherTests.TestApplication.class)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.task.scheduling.enabled=false",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration,com.ejada.kafka_starter.config.KafkaAutoConfiguration"
})
class TenantEventPublisherTests {

    @SpringBootApplication
    @ComponentScan(excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = OutboxPublisher.class))
    static class TestApplication {}

    @Autowired
    TenantEventWriter writer;

    @Autowired
    TenantEventPublisher publisher;

    @Autowired
    TenantOutboxEventRepository repository;

    @Test
    void insertAndPublishWithoutKafkaMarksSent() {
        UUID tenantId = UUID.randomUUID();
        writer.write(tenantId, "test-topic", Map.of("hello", "world"));

        publisher.publish();

        TenantOutboxEvent event = repository.findAll().getFirst();
        assertThat(event.getStatus()).isEqualTo(TenantOutboxEvent.Status.SENT);
        assertThat(event.getAttempts()).isEqualTo(1);
    }
}
