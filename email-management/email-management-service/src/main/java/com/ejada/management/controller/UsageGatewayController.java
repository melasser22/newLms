package com.ejada.management.controller;

import com.ejada.management.dto.UsageReport;
import com.ejada.management.service.AuditLogger;
import com.ejada.management.service.TenantRateLimiter;
import com.ejada.management.service.UsageGatewayService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/usage")
public class UsageGatewayController {

  private final UsageGatewayService service;
  private final TenantRateLimiter rateLimiter;
  private final AuditLogger auditLogger;

  public UsageGatewayController(
      UsageGatewayService service, TenantRateLimiter rateLimiter, AuditLogger auditLogger) {
    this.service = service;
    this.rateLimiter = rateLimiter;
    this.auditLogger = auditLogger;
  }

  @GetMapping
  public UsageReport usage(
      @PathVariable String tenantId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    rateLimiter.assertWithinQuota(tenantId, "usage-report");
    auditLogger.logTenantAction(tenantId, "USAGE_REPORT", from + ":" + to);
    return service.fetchUsage(tenantId, from, to);
  }
}
