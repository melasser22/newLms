package com.ejada.template.dto;

import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class UpdateTemplateRequest {

  @Size(max = 512)
  private String description;

  private Map<String, Object> metadata;

  private List<AttachmentMetadataDto> defaultAttachments;
}
