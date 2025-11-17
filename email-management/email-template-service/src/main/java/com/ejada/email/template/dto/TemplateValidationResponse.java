package com.ejada.email.template.dto;

import java.util.Set;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TemplateValidationResponse {
  boolean valid;
  Set<String> missingVariables;
  Set<String> unexpectedVariables;
}
