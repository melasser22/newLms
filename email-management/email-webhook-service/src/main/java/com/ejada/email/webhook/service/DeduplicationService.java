package com.ejada.email.webhook.service;

import com.ejada.email.webhook.SendgridWebhookProperties;
import java.time.Duration;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class DeduplicationService {

  private static final Logger log = LoggerFactory.getLogger(DeduplicationService.class);

  private final StringRedisTemplate redisTemplate;
  private final SendgridWebhookProperties properties;

  public DeduplicationService(
      StringRedisTemplate redisTemplate, SendgridWebhookProperties properties) {
    this.redisTemplate = redisTemplate;
    this.properties = properties;
  }

  public String resolveEventId(String providedEventId, String messageId) {
    if (providedEventId != null && !providedEventId.isBlank()) {
      return providedEventId;
    }
    return "fallback-" + messageId + "-" + UUID.randomUUID();
  }

  public boolean markIfFirst(String eventId) {
    try {
      Boolean success =
          redisTemplate
              .opsForValue()
              .setIfAbsent(
                  redisKey(eventId),
                  "1",
                  Duration.ofSeconds(properties.getDeduplicationTtlSeconds()));
      return Boolean.TRUE.equals(success);
    } catch (Exception ex) {
      log.warn("Redis unavailable, proceeding without deduplication", ex);
      return true;
    }
  }

  private String redisKey(String eventId) {
    return "sendgrid:event:" + eventId;
  }
}
