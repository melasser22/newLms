package com.ejada.usage.controller;

import com.ejada.usage.dto.UsageReportDto;
import com.ejada.usage.service.UsageService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/usage")
public class UsageController {

  private final UsageService service;

  public UsageController(UsageService service) {
    this.service = service;
  }

  @GetMapping
  public UsageReportDto report(
      @PathVariable String tenantId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return service.report(tenantId, from, to);
  }
}
