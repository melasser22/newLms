package com.ejada.email.template.messaging.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class WebhookEventMessage {
  String tenantId;
  List<Map<String, Object>> events;
  Instant receivedAt;
}
