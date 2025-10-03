package com.ejada.analytics.controller;

import com.ejada.analytics.dto.AnalyticsPeriod;
import com.ejada.analytics.dto.CostForecastResponse;
import com.ejada.analytics.dto.FeatureAdoptionResponse;
import com.ejada.analytics.dto.UsageSummaryResponse;
import com.ejada.analytics.service.AnalyticsService;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics/tenants")
@Validated
public class AnalyticsController {

  private final AnalyticsService analyticsService;

  public AnalyticsController(AnalyticsService analyticsService) {
    this.analyticsService = analyticsService;
  }

  @GetMapping("/{tenantId}/usage-summary")
  public ResponseEntity<UsageSummaryResponse> getUsageSummary(
      @PathVariable("tenantId") @Min(1) Long tenantId,
      @RequestParam(name = "period", defaultValue = "MONTHLY") AnalyticsPeriod period) {
    return ResponseEntity.ok(analyticsService.getUsageSummary(tenantId, period));
  }

  @GetMapping("/{tenantId}/feature-adoption")
  public ResponseEntity<FeatureAdoptionResponse> getFeatureAdoption(
      @PathVariable("tenantId") @Min(1) Long tenantId) {
    return ResponseEntity.ok(analyticsService.getFeatureAdoption(tenantId));
  }

  @GetMapping("/{tenantId}/cost-forecast")
  public ResponseEntity<CostForecastResponse> getCostForecast(
      @PathVariable("tenantId") @Min(1) Long tenantId) {
    return ResponseEntity.ok(analyticsService.getCostForecast(tenantId));
  }
}
