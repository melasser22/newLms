package com.ejada.tenant.events;

import com.ejada.tenant.events.entity.TenantOutboxEvent;
import com.ejada.tenant.events.entity.TenantOutboxEvent.Status;
import com.ejada.tenant.events.repo.TenantOutboxEventRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

/** Polls the outbox table and publishes tenant events. */
public class TenantEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(TenantEventPublisher.class);

    private final TenantOutboxEventRepository repository;
    private final ObjectProvider<KafkaTemplate<String, String>> kafkaProvider;
    private final TenantEventsProperties properties;

    public TenantEventPublisher(TenantOutboxEventRepository repository,
                                ObjectProvider<KafkaTemplate<String, String>> kafkaProvider,
                                TenantEventsProperties properties) {
        this.repository = repository;
        this.kafkaProvider = kafkaProvider;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${tenant.events.poll-interval:5s}")
    @Transactional
    public void publish() {
        List<TenantOutboxEvent> rows = repository.findByStatusAndAvailableAtLessThanEqualOrderById(
            Status.NEW, Instant.now(), PageRequest.of(0, properties.getBatchSize()));
        KafkaTemplate<String, String> kafka = kafkaProvider.getIfAvailable();
        for (TenantOutboxEvent row : rows) {
            boolean success = false;
            try {
                if (kafka != null) {
                    ProducerRecord<String, String> record = new ProducerRecord<>(row.getType(), row.getPayload());
                    String tenantId = row.getTenantId();
                    if (tenantId != null) {
                        record.headers().add("X-Tenant-Id", tenantId.getBytes(StandardCharsets.UTF_8));
                    }
                    kafka.send(record);
                } else {
                    log.info("Tenant event {}: {}", row.getType(), row.getPayload());
                }
                success = true;
            } catch (Exception ex) {
                log.warn("Failed to publish tenant event {}", row.getId(), ex);
            }
            row.setAttempts(row.getAttempts() + 1);
            if (success) {
                row.setStatus(Status.SENT);
            }
            repository.save(row);
        }
    }
}
