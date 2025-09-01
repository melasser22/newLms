package com.lms.tenant.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "PT1S")
    public void publish() {
        String tenantId = UUID.randomUUID().toString();
        try (MDC.MDCCloseable ignored = MDC.putCloseable("tenantId", tenantId)) {
            var message = MessageBuilder.withPayload("{}")
                    .setHeader(KafkaHeaders.TOPIC, "tenant.events")
                    .setHeader("X-Tenant-Id", tenantId)
                    .build();
            kafkaTemplate.send(message);
            log.info("published tenant event");
        }
    }
}
