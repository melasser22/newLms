package com.ejada.sms.sending.messaging;

import com.ejada.sms.sending.dto.SmsSendRequest;
import java.time.Instant;
import java.util.UUID;

public record SmsEnvelope(
    String id,
    String tenantId,
    String recipient,
    String senderId,
    String templateCode,
    String locale,
    String message,
    String clientReference,
    Instant createdAt
) {

  public static SmsEnvelope from(String tenantId, SmsSendRequest request) {
    return new SmsEnvelope(
        UUID.randomUUID().toString(),
        tenantId,
        request.recipient(),
        request.senderId(),
        request.templateCode(),
        request.locale(),
        request.message(),
        request.clientReference(),
        Instant.now());
  }
}
