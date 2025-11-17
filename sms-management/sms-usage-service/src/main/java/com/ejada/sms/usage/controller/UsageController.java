package com.ejada.sms.usage.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/usage")
public class UsageController {

  @GetMapping
  public Map<String, Object> usage(
      @PathVariable String tenantId,
      @RequestParam(defaultValue = "7") int days
  ) {
    return Map.of(
        "tenantId", tenantId,
        "windowDays", days,
        "totals",
            List.of(
                Map.of("date", LocalDate.now().minusDays(1), "sent", 42, "delivered", 40, "failed", 2),
                Map.of("date", LocalDate.now(), "sent", 10, "delivered", 9, "failed", 1))
    );
  }
}
