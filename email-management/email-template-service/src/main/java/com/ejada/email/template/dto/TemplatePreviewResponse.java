package com.ejada.email.template.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TemplatePreviewResponse {
  String htmlBody;
  String textBody;
}
