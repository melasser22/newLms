package com.ejada.management.controller;

import com.ejada.management.dto.TenantPortalView;
import com.ejada.management.service.AuditLogger;
import com.ejada.management.service.TenantExperienceService;
import com.ejada.management.service.TenantRateLimiter;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/portal")
public class TenantPortalController {

  private final TenantExperienceService tenantExperienceService;
  private final TenantRateLimiter rateLimiter;
  private final AuditLogger auditLogger;

  public TenantPortalController(
      TenantExperienceService tenantExperienceService,
      TenantRateLimiter rateLimiter,
      AuditLogger auditLogger) {
    this.tenantExperienceService = tenantExperienceService;
    this.rateLimiter = rateLimiter;
    this.auditLogger = auditLogger;
  }

  @GetMapping
  public TenantPortalView portal(
      @PathVariable String tenantId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    LocalDate effectiveTo = to != null ? to : LocalDate.now();
    LocalDate effectiveFrom = from != null ? from : effectiveTo.minusDays(30);
    rateLimiter.assertWithinQuota(tenantId, "portal-view");
    TenantPortalView view = tenantExperienceService.buildTenantPortal(tenantId, effectiveFrom, effectiveTo);
    auditLogger.logTenantAction(tenantId, "PORTAL_VIEW", effectiveFrom + "-" + effectiveTo);
    return view;
  }
}
