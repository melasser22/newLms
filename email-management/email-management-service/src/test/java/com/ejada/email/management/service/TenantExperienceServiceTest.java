package com.ejada.email.management.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ejada.email.management.dto.TemplateSummary;
import com.ejada.email.management.dto.UsageMetric;
import com.ejada.email.management.dto.UsageReport;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TenantExperienceServiceTest {

  private final TemplateGatewayService templateGatewayService = mock(TemplateGatewayService.class);
  private final UsageGatewayService usageGatewayService = mock(UsageGatewayService.class);
  private final GlobalConfigService globalConfigService = mock(GlobalConfigService.class);

  @Test
  void shouldBuildPortalWithEnabledFeatures() {
    LocalDate from = LocalDate.now().minusDays(7);
    LocalDate to = LocalDate.now();
    List<TemplateSummary> templates =
        List.of(new TemplateSummary(1L, "name", "en", false, Instant.parse("2024-01-01T00:00:00Z")));
    UsageReport usageReport =
        new UsageReport("tenant-1", List.of(new UsageMetric(from, 10, 0, 0)));

    when(globalConfigService.isFeatureEnabled("templates")).thenReturn(true);
    when(globalConfigService.isFeatureEnabled("usage")).thenReturn(true);
    when(globalConfigService.tenantSettings("tenant-1")).thenReturn(Map.of("color", "green"));
    when(templateGatewayService.fetchTemplates("tenant-1")).thenReturn(templates);
    when(usageGatewayService.fetchUsage("tenant-1", from, to)).thenReturn(usageReport);

    TenantExperienceService service =
        new TenantExperienceService(templateGatewayService, usageGatewayService, globalConfigService);

    var portal = service.buildTenantPortal("tenant-1", from, to);

    assertThat(portal.tenantId()).isEqualTo("tenant-1");
    assertThat(portal.templates()).containsExactlyElementsOf(templates);
    assertThat(portal.usage()).isEqualTo(usageReport);
    assertThat(portal.settings()).containsEntry("color", "green");

    verify(templateGatewayService).fetchTemplates("tenant-1");
    verify(usageGatewayService).fetchUsage("tenant-1", from, to);
  }

  @Test
  void shouldSkipDisabledFeatures() {
    when(globalConfigService.isFeatureEnabled("templates")).thenReturn(false);
    when(globalConfigService.isFeatureEnabled("usage")).thenReturn(false);
    when(globalConfigService.tenantSettings("tenant-2")).thenReturn(Map.of());

    TenantExperienceService service =
        new TenantExperienceService(templateGatewayService, usageGatewayService, globalConfigService);

    var portal = service.buildTenantPortal("tenant-2", LocalDate.MIN, LocalDate.MAX);

    assertThat(portal.templates()).isEmpty();
    assertThat(portal.usage()).isNull();

    verifyNoInteractions(templateGatewayService, usageGatewayService);
  }
}
