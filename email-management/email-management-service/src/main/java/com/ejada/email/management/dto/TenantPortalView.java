package com.ejada.management.dto;

import java.util.List;
import java.util.Map;

public record TenantPortalView(
    String tenantId, List<TemplateSummary> templates, UsageReport usage, Map<String, String> settings) {

  public TenantPortalView {
    templates = templates == null ? List.of() : List.copyOf(templates);
    settings = settings == null ? Map.of() : Map.copyOf(settings);
  }
}
