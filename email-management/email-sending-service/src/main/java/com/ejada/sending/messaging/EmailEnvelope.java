package com.ejada.sending.messaging;

import com.ejada.sending.dto.AttachmentMetadataDto;
import com.ejada.sending.dto.EmailSendRequest;
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
    Instant createdAt) {

  public static EmailEnvelope from(String tenantId, EmailSendRequest request) {
    return new EmailEnvelope(
        UUID.randomUUID().toString(),
        tenantId,
        request.templateKey(),
        request.to(),
        request.cc(),
        request.bcc(),
        request.dynamicData(),
        request.attachments(),
        request.mode(),
        Instant.now());
  }
}
