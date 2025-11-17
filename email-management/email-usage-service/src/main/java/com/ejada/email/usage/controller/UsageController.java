package com.ejada.usage.controller;

import com.ejada.usage.domain.AnomalyAlert;
import com.ejada.usage.domain.QuotaStatus;
import com.ejada.usage.domain.UsageReportRow;
import com.ejada.usage.domain.UsageSummary;
import com.ejada.usage.domain.UsageTrendPoint;
import com.ejada.usage.service.UsageAnalyticsService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class UsageController {

  private final UsageAnalyticsService usageAnalyticsService;

  public UsageController(UsageAnalyticsService usageAnalyticsService) {
    this.usageAnalyticsService = usageAnalyticsService;
  }

  @GetMapping("/tenants/{tenantId}/usage/summary")
  public ResponseEntity<UsageSummary> usageSummary(
      @PathVariable String tenantId,
      @RequestParam(value = "from", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate from,
      @RequestParam(value = "to", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate to) {
    LocalDate resolvedTo = to != null ? to : LocalDate.now();
    LocalDate resolvedFrom = from != null ? from : resolvedTo.minusDays(30);
    return ResponseEntity.ok(usageAnalyticsService.summarize(tenantId, resolvedFrom, resolvedTo));
  }

  @GetMapping("/tenants/{tenantId}/usage/trends")
  public ResponseEntity<List<UsageTrendPoint>> usageTrends(
      @PathVariable String tenantId,
      @RequestParam(value = "from", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate from,
      @RequestParam(value = "to", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate to) {
    LocalDate resolvedTo = to != null ? to : LocalDate.now();
    LocalDate resolvedFrom = from != null ? from : resolvedTo.minusDays(30);
    return ResponseEntity.ok(usageAnalyticsService.trend(tenantId, resolvedFrom, resolvedTo));
  }

  @GetMapping("/tenants/{tenantId}/usage/quota")
  public ResponseEntity<QuotaStatus> quotaStatus(@PathVariable String tenantId) {
    return ResponseEntity.ok(usageAnalyticsService.quotaStatus(tenantId));
  }

  @GetMapping("/tenants/{tenantId}/usage/anomalies")
  public ResponseEntity<List<AnomalyAlert>> anomalies(@PathVariable String tenantId) {
    return ResponseEntity.ok(usageAnalyticsService.detectAnomalies(tenantId));
  }

  @GetMapping("/admin/usage/reports/daily")
  public ResponseEntity<List<UsageReportRow>> dailyReport(
      @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return ResponseEntity.ok(usageAnalyticsService.dailyReport(date));
  }
}
