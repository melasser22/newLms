package com.ejada.sms.webhook.controller;

import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhook/{tenantId}")
public class WebhookController {

  private final Map<String, Instant> seenEvents = new ConcurrentHashMap<>();

  @PostMapping
  @ResponseStatus(HttpStatus.ACCEPTED)
  public Map<String, Object> ingest(
      @PathVariable String tenantId, @Valid @RequestBody Map<String, Object> payload) {
    String eventId = (String) payload.getOrDefault("eventId", "unknown");
    if (seenEvents.containsKey(tenantId + ':' + eventId)) {
      return Map.of("status", "duplicate", "tenantId", tenantId, "eventId", eventId);
    }
    seenEvents.put(tenantId + ':' + eventId, Instant.now());
    return Map.of("status", "accepted", "tenantId", tenantId, "eventId", eventId, "payload", payload);
  }
}
