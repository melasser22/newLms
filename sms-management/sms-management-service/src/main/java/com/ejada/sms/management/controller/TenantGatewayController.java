package com.ejada.sms.management.controller;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}")
public class TenantGatewayController {

  @GetMapping("/sms/summary")
  public Map<String, Object> summary(@PathVariable String tenantId) {
    return Map.of(
        "tenantId", tenantId,
        "templatesEndpoint", "/api/v1/tenants/" + tenantId + "/templates",
        "sendEndpoint", "/api/v1/tenants/" + tenantId + "/sms/send",
        "usageEndpoint", "/api/v1/tenants/" + tenantId + "/usage"
    );
  }

  @PostMapping("/quota/check")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public Map<String, Object> checkQuota(@PathVariable String tenantId, @RequestBody Map<String, Object> request) {
    return Map.of("tenantId", tenantId, "allowed", Boolean.TRUE, "details", request);
  }
}
