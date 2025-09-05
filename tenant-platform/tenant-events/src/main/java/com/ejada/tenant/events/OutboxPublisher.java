package com.ejada.tenant.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private final ObjectProvider<KafkaTemplate<String, String>> kafkaProvider;

    public OutboxPublisher(ObjectProvider<KafkaTemplate<String, String>> kafkaProvider) {
        this.kafkaProvider = kafkaProvider;
    }

    @Scheduled(fixedDelayString = "PT1S")
    public void publish() {
        KafkaTemplate<String, String> kafka = kafkaProvider.getIfAvailable();
        if (kafka == null) {
            log.debug("Kafka template not available, skipping tenant event publish");
            return;
        }
        String tenantId = UUID.randomUUID().toString();
        try (MDC.MDCCloseable ignored = MDC.putCloseable("tenantId", tenantId)) {
            var message = MessageBuilder.withPayload("{}")
                    .setHeader(KafkaHeaders.TOPIC, "tenant.events")
                    .setHeader("X-Tenant-Id", tenantId)
                    .build();
            try {
                kafka.send(message);
                log.info("published tenant event");
            } catch (Exception ex) {
                log.warn("Failed to publish tenant event", ex);
            }
        }
    }
}
