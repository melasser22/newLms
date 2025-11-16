package com.ejada.template.service.impl;

import com.ejada.template.config.SendGridProperties;
import com.ejada.template.messaging.model.WebhookEventMessage;
import com.ejada.template.messaging.producer.WebhookEventProducer;
import com.ejada.template.service.WebhookService;
import com.ejada.template.service.support.WebhookDeduplicationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sendgrid.helpers.eventwebhook.EventWebhook;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookServiceImpl implements WebhookService {

  private static final Duration DEDUP_TTL = Duration.ofHours(1);

  private final ObjectMapper objectMapper;
  private final SendGridProperties sendGridProperties;
  private final WebhookEventProducer webhookEventProducer;
  private final WebhookDeduplicationService deduplicationService;
  private final EventWebhook eventWebhook = new EventWebhook();

  @Override
  public void handleWebhook(String tenantId, String payload, String signature, String timestamp) {
    verifySignature(payload, signature, timestamp);
    List<Map<String, Object>> events = parsePayload(payload);
    List<Map<String, Object>> filtered =
        events.stream()
            .filter(event -> !deduplicationService.isDuplicate(tenantId, dedupKey(event), DEDUP_TTL))
            .collect(Collectors.toList());
    if (filtered.isEmpty()) {
      log.info("All webhook events were duplicates for tenant {}", tenantId);
      return;
    }
    WebhookEventMessage message = WebhookEventMessage.builder()
        .tenantId(tenantId)
        .events(filtered)
        .receivedAt(Instant.now())
        .build();
    webhookEventProducer.publish(message);
  }

  private void verifySignature(String payload, String signature, String timestamp) {
    String publicKeyValue = sendGridProperties.webhookPublicKey();
    if (publicKeyValue == null || publicKeyValue.isBlank()) {
      throw new IllegalStateException("SendGrid webhook public key is not configured");
    }
    try {
      boolean valid =
          eventWebhook.VerifySignature(
              eventWebhook.ConvertPublicKeyToECDSA(publicKeyValue), payload, signature, timestamp);
      if (!valid) {
        throw new IllegalArgumentException("Invalid webhook signature");
      }
    } catch (RuntimeException ex) {
      throw new IllegalArgumentException("Unable to verify webhook signature", ex);
    }
  }

  private List<Map<String, Object>> parsePayload(String payload) {
    try {
      return objectMapper.readValue(payload, new TypeReference<>() {});
    } catch (IOException e) {
      throw new IllegalArgumentException("Unable to parse webhook payload", e);
    }
  }

  private String dedupKey(Map<String, Object> event) {
    Object id = event.getOrDefault("event_id", event.get("sg_message_id"));
    Object type = event.get("event");
    Object timestamp = event.get("timestamp");
    return id + ":" + type + ":" + timestamp;
  }
}
