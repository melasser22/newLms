package com.ejada.template.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Data;

@Data
public class TemplateVersionCreateRequest {
  @NotBlank private String subject;
  @NotBlank private String htmlBody;
  private String textBody;
  @NotNull private Set<String> allowedVariables;
  private Map<String, Object> metadata;
  private List<AttachmentMetadataDto> attachments;
  private Long sourceVersionId;
}
