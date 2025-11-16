package com.ejada.template.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.Data;

@Data
public class TemplatePreviewRequest {
  @NotNull private Map<String, Object> dynamicData;
}
