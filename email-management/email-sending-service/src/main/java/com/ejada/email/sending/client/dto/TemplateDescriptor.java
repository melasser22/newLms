package com.ejada.email.sending.client.dto;

import com.ejada.email.sending.dto.AttachmentMetadataDto;
import java.util.List;

public record TemplateDescriptor(
    String templateKey, List<AttachmentMetadataDto> defaultAttachments, boolean sandboxEnabled) {

  public TemplateDescriptor {
    defaultAttachments =
        defaultAttachments == null ? List.of() : List.copyOf(defaultAttachments);
  }

  @Override
  public List<AttachmentMetadataDto> defaultAttachments() {
    return List.copyOf(defaultAttachments);
  }
}
