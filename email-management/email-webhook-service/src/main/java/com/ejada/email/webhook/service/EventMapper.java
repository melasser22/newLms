package com.ejada.email.webhook.service;

import com.ejada.email.webhook.model.EmailEvent;
import com.ejada.email.webhook.model.EmailEventType;
import com.ejada.email.webhook.model.EmailLogStatus;
import com.ejada.email.webhook.model.SendgridEventRequest;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class EventMapper {

  public EmailEventType mapEventType(String eventName) {
    if (eventName == null) {
      return EmailEventType.UNKNOWN;
    }
    return switch (eventName.toLowerCase(Locale.ROOT)) {
      case "processed" -> EmailEventType.PROCESSED;
      case "delivered" -> EmailEventType.DELIVERED;
      case "bounce" -> EmailEventType.BOUNCED;
      case "spamreport" -> EmailEventType.SPAM_REPORTED;
      case "open" -> EmailEventType.OPENED;
      case "click" -> EmailEventType.CLICKED;
      case "unsubscribe" -> EmailEventType.UNSUBSCRIBED;
      case "group_unsubscribe" -> EmailEventType.GROUP_UNSUBSCRIBED;
      case "deferred" -> EmailEventType.DEFERRED;
      case "dropped" -> EmailEventType.DROPPED;
      default -> EmailEventType.UNKNOWN;
    };
  }

  public EmailLogStatus mapLogStatus(EmailEventType type) {
    return switch (type) {
      case DELIVERED -> EmailLogStatus.DELIVERED;
      case BOUNCED -> EmailLogStatus.BOUNCED;
      case DROPPED -> EmailLogStatus.DROPPED;
      case DEFERRED -> EmailLogStatus.DEFERRED;
      case SPAM_REPORTED -> EmailLogStatus.SPAM_REPORTED;
      case UNSUBSCRIBED, GROUP_UNSUBSCRIBED -> EmailLogStatus.UNSUBSCRIBED;
      default -> EmailLogStatus.UNKNOWN;
    };
  }

  public EmailEvent toEmailEvent(
      SendgridEventRequest request, String tenantId, String eventId, boolean duplicate) {
    Map<String, Object> metadata = new HashMap<>();
    if (request.getReason() != null) {
      metadata.put("reason", request.getReason());
    }
    if (request.getUrl() != null) {
      metadata.put("url", request.getUrl());
    }
    if (request.getIp() != null) {
      metadata.put("ip", request.getIp());
    }
    if (request.getUserAgent() != null) {
      metadata.put("userAgent", request.getUserAgent());
    }
    if (request.getCategory() != null) {
      metadata.put("category", request.getCategory());
    }
    metadata.putAll(request.getAdditionalFields());

    EmailEventType type = mapEventType(request.getEvent());
    Instant processedAt = Instant.now();
    return new EmailEvent(
        UUID.randomUUID(),
        eventId,
        type,
        request.getEmail(),
        request.getMessageId(),
        tenantId,
        request.occurredAt(),
        processedAt,
        metadata,
        duplicate);
  }
}
