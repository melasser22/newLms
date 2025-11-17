package com.ejada.push.usage.controller;

import com.ejada.push.usage.service.UsageEvent;
import com.ejada.push.usage.service.UsageService;
import com.ejada.push.usage.service.UsageSummary;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/usage")
public class UsageController {

  private final UsageService usageService;

  public UsageController(UsageService usageService) {
    this.usageService = usageService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void recordEvent(@PathVariable String tenantId, @Valid @RequestBody UsageEvent event) {
    usageService.recordEvent(tenantId, event);
  }

  @GetMapping("/daily")
  public UsageSummary summary(@PathVariable String tenantId) {
    return usageService.summarize(tenantId);
  }
}
