package com.ejada.email.sending.messaging;

import com.ejada.email.sending.dto.AttachmentMetadataDto;
import com.ejada.email.sending.dto.EmailSendRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record EmailEnvelope(
    String id,
    String tenantId,
    String templateKey,
    List<String> to,
    List<String> cc,
    List<String> bcc,
    Map<String, Object> dynamicData,
    List<AttachmentMetadataDto> attachments,
    EmailSendRequest.SendMode mode,
    Instant createdAt,
    String idempotencyKey) {

  public EmailEnvelope {
    to = to == null ? List.of() : List.copyOf(to);
    cc = cc == null ? List.of() : List.copyOf(cc);
    bcc = bcc == null ? List.of() : List.copyOf(bcc);
    dynamicData = dynamicData == null ? Map.of() : Map.copyOf(dynamicData);
    attachments = attachments == null ? List.of() : List.copyOf(attachments);
  }

  public static EmailEnvelope from(String tenantId, EmailSendRequest request) {
    return new EmailEnvelope(
        UUID.randomUUID().toString(),
        tenantId,
        request.templateKey(),
        request.to(),
        request.cc(),
        request.bcc(),
        request.dynamicData() == null ? Map.of() : request.dynamicData(),
        request.attachments() == null ? List.of() : request.attachments(),
        request.mode(),
        Instant.now(),
        request.idempotencyKey());
  }
}
