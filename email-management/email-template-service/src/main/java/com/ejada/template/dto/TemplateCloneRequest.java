package com.ejada.template.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TemplateCloneRequest {
  @NotBlank private String name;
  @NotBlank private String locale;
  private boolean includeVersions = true;
}
