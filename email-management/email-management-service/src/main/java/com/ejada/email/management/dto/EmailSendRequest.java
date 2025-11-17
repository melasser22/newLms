package com.ejada.management.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

public record EmailSendRequest(
    @NotBlank String templateKey,
    @NotEmpty List<@Email String> recipients,
    Map<String, Object> dynamicData,
    List<AttachmentRequest> attachments,
    @NotNull SendMode mode,
    String idempotencyKey) {

  public EmailSendRequest {
    recipients = List.copyOf(recipients);
    dynamicData = dynamicData == null ? Map.of() : Map.copyOf(dynamicData);
    attachments = attachments == null ? List.of() : List.copyOf(attachments);
  }

  public enum SendMode {
    PRODUCTION,
    TEST,
    DRAFT
  }

  public record AttachmentRequest(
      @NotBlank String fileName, @NotBlank String contentType, @NotBlank String url) {

    public AttachmentRequest {
      // record validation already enforced by annotations
    }
  }
}
