package com.ejada.template.dto;

import com.ejada.template.domain.enums.TemplateVersionStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TemplateVersionDto {
  Long id;
  int versionNumber;
  String subject;
  String htmlBody;
  String textBody;
  Set<String> allowedVariables;
  Map<String, Object> metadata;
  TemplateVersionStatus status;
  Instant publishedAt;
  List<AttachmentMetadataDto> attachments;
}
