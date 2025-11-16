package com.ejada.management.dto;

import java.util.List;
import java.util.Map;

public record TenantPortalView(
    String tenantId, List<TemplateSummary> templates, UsageReport usage, Map<String, String> settings) {}
