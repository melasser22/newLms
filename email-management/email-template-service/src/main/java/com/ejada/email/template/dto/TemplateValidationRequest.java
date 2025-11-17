package com.ejada.template.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.Data;

@Data
public class TemplateValidationRequest {
  @NotNull private Map<String, Object> dynamicData;
}
