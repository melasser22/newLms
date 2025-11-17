package com.ejada.email.template.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class CreateTemplateRequest {
  @NotBlank
  private String name;

  @NotBlank
  @Size(max = 32)
  private String locale;

  @Size(max = 512)
  private String description;

  private Map<String, Object> metadata;

  private List<AttachmentMetadataDto> defaultAttachments;
}
