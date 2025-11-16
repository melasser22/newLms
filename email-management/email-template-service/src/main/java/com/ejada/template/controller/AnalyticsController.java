package com.ejada.template.controller;

import com.ejada.common.dto.BaseResponse;
import com.ejada.template.dto.EmailStatsResponse;
import com.ejada.template.service.EmailAnalyticsService;
import java.time.Instant;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

  private final EmailAnalyticsService analyticsService;

  public AnalyticsController(EmailAnalyticsService analyticsService) {
    this.analyticsService = analyticsService;
  }

  @GetMapping("/email-stats")
  public BaseResponse<EmailStatsResponse> getEmailStats(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
    return BaseResponse.success(analyticsService.getEmailStats(from, to));
  }
}
