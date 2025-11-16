package com.ejada.webhook.messaging;

import java.time.Instant;
import java.util.Map;

public record WebhookEventMessage(
    String tenantId, String eventType, String sgMessageId, Map<String, Object> payload, Instant occurredAt) {}
