package com.ejada.email.management.service;

import com.ejada.email.management.dto.TenantPortalView;
import com.ejada.email.management.dto.TemplateSummary;
import com.ejada.email.management.dto.UsageReport;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class TenantExperienceService {

  private final TemplateGatewayService templateGatewayService;
  private final UsageGatewayService usageGatewayService;
  private final GlobalConfigService globalConfigService;

  public TenantExperienceService(
      TemplateGatewayService templateGatewayService,
      UsageGatewayService usageGatewayService,
      GlobalConfigService globalConfigService) {
    this.templateGatewayService = templateGatewayService;
    this.usageGatewayService = usageGatewayService;
    this.globalConfigService = globalConfigService;
  }

  public TenantPortalView buildTenantPortal(String tenantId, LocalDate from, LocalDate to) {
    List<TemplateSummary> templates = List.of();
    if (globalConfigService.isFeatureEnabled("templates")) {
      templates = templateGatewayService.fetchTemplates(tenantId);
    }

    UsageReport usage = null;
    if (globalConfigService.isFeatureEnabled("usage")) {
      usage = usageGatewayService.fetchUsage(tenantId, from, to);
    }

    Map<String, String> settings = globalConfigService.tenantSettings(tenantId);
    return new TenantPortalView(tenantId, templates, usage, settings);
  }
}
