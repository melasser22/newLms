package com.ejada.webhook.service.impl;

import com.ejada.webhook.dto.SendGridEvent;
import com.ejada.webhook.dto.SendGridWebhookRequest;
import com.ejada.webhook.messaging.WebhookEventMessage;
import com.ejada.webhook.service.DeduplicationService;
import com.ejada.webhook.service.WebhookService;
import java.time.Instant;
import java.util.Map;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class WebhookServiceImpl implements WebhookService {

  private final KafkaTemplate<String, WebhookEventMessage> kafkaTemplate;
  private final DeduplicationService deduplicationService;

  public WebhookServiceImpl(
      KafkaTemplate<String, WebhookEventMessage> kafkaTemplate,
      DeduplicationService deduplicationService) {
    this.kafkaTemplate = kafkaTemplate;
    this.deduplicationService = deduplicationService;
  }

  @Override
  public void handle(SendGridWebhookRequest request) {
    for (SendGridEvent event : request.events()) {
      if (deduplicationService.seen(event.getSgMessageId())) {
        continue;
      }
      WebhookEventMessage message =
          new WebhookEventMessage(
              (String) event.getCustomArgs().getOrDefault("tenantId", "unknown"),
              event.getEvent(),
              event.getSgMessageId(),
              Map.of(
                  "email", event.getEmail(),
                  "timestamp", event.getTimestamp() == null ? Instant.now() : event.getTimestamp()),
              event.getTimestamp() == null ? Instant.now() : event.getTimestamp());
      kafkaTemplate.send("email.events", message.tenantId(), message);
    }
  }
}
