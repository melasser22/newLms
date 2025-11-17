package com.ejada.email.template.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TemplateDto {
  Long id;
  String name;
  String locale;
  String description;
  boolean archived;
  Map<String, Object> metadata;
  List<AttachmentMetadataDto> defaultAttachments;
  List<TemplateVersionDto> versions;
  Instant createdAt;
  Instant updatedAt;
}
