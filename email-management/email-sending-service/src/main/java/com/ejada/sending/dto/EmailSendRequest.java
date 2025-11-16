package com.ejada.sending.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

public record EmailSendRequest(
    @NotBlank String templateKey,
    @NotEmpty List<@Email String> to,
    List<@Email String> cc,
    List<@Email String> bcc,
    Map<String, Object> dynamicData,
    @Valid List<AttachmentMetadataDto> attachments,
    @NotNull SendMode mode,
    String idempotencyKey) {

  public enum SendMode {
    PRODUCTION,
    TEST,
    DRAFT
  }
}
