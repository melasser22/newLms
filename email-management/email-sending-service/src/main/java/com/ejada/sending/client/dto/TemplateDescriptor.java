package com.ejada.sending.client.dto;

import com.ejada.sending.dto.AttachmentMetadataDto;
import java.util.List;

public record TemplateDescriptor(
    String templateKey, List<AttachmentMetadataDto> defaultAttachments, boolean sandboxEnabled) {

  public TemplateDescriptor {
    defaultAttachments = defaultAttachments == null ? List.of() : defaultAttachments;
  }
}
