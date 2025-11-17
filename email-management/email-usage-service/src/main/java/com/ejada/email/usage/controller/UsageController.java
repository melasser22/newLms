package com.ejada.email.usage.controller;

import com.ejada.common.context.ContextManager;
import com.ejada.email.usage.domain.AnomalyAlert;
import com.ejada.email.usage.domain.QuotaStatus;
import com.ejada.email.usage.domain.UsageReportRow;
import com.ejada.email.usage.domain.UsageSummary;
import com.ejada.email.usage.domain.UsageTrendPoint;
import com.ejada.email.usage.service.UsageAnalyticsService;
import com.ejada.starter_core.tenant.RequireTenant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usage")
@RequireTenant
public class UsageController {

  private final UsageAnalyticsService usageAnalyticsService;

  public UsageController(UsageAnalyticsService usageAnalyticsService) {
    this.usageAnalyticsService = usageAnalyticsService;
  }

  @GetMapping("/summary")
  public ResponseEntity<UsageSummary> usageSummary(
      @RequestParam(value = "from", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate from,
      @RequestParam(value = "to", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate to) {
    String tenantId = Objects.requireNonNull(ContextManager.Tenant.get(), "tenantId is required");
    LocalDate resolvedTo = to != null ? to : LocalDate.now();
    LocalDate resolvedFrom = from != null ? from : resolvedTo.minusDays(30);
    return ResponseEntity.ok(usageAnalyticsService.summarize(tenantId, resolvedFrom, resolvedTo));
  }

  @GetMapping("/trends")
  public ResponseEntity<List<UsageTrendPoint>> usageTrends(
      @RequestParam(value = "from", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate from,
      @RequestParam(value = "to", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate to) {
    String tenantId = Objects.requireNonNull(ContextManager.Tenant.get(), "tenantId is required");
    LocalDate resolvedTo = to != null ? to : LocalDate.now();
    LocalDate resolvedFrom = from != null ? from : resolvedTo.minusDays(30);
    return ResponseEntity.ok(usageAnalyticsService.trend(tenantId, resolvedFrom, resolvedTo));
  }

  @GetMapping("/quota")
  public ResponseEntity<QuotaStatus> quotaStatus() {
    String tenantId = Objects.requireNonNull(ContextManager.Tenant.get(), "tenantId is required");
    return ResponseEntity.ok(usageAnalyticsService.quotaStatus(tenantId));
  }

  @GetMapping("/anomalies")
  public ResponseEntity<List<AnomalyAlert>> anomalies() {
    String tenantId = Objects.requireNonNull(ContextManager.Tenant.get(), "tenantId is required");
    return ResponseEntity.ok(usageAnalyticsService.detectAnomalies(tenantId));
  }

  @GetMapping("/admin/reports/daily")
  public ResponseEntity<List<UsageReportRow>> dailyReport(
      @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return ResponseEntity.ok(usageAnalyticsService.dailyReport(date));
  }
}
