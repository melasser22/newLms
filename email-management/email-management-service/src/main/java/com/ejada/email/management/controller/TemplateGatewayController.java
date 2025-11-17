package com.ejada.email.management.controller;

import com.ejada.email.management.dto.TemplateSummary;
import com.ejada.email.management.dto.TemplateSyncRequest;
import com.ejada.email.management.service.AuditLogger;
import com.ejada.email.management.service.TemplateGatewayService;
import com.ejada.email.management.service.TenantRateLimiter;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/templates")
public class TemplateGatewayController {

  private final TemplateGatewayService service;
  private final TenantRateLimiter rateLimiter;
  private final AuditLogger auditLogger;

  public TemplateGatewayController(
      TemplateGatewayService service, TenantRateLimiter rateLimiter, AuditLogger auditLogger) {
    this.service = service;
    this.rateLimiter = rateLimiter;
    this.auditLogger = auditLogger;
  }

  @GetMapping
  public List<TemplateSummary> listTemplates(@PathVariable String tenantId) {
    rateLimiter.assertWithinQuota(tenantId, "template-list");
    auditLogger.logTenantAction(tenantId, "TEMPLATE_LIST", "fetch");
    return service.fetchTemplates(tenantId);
  }

  @PostMapping("/sync")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void syncTemplates(
      @PathVariable String tenantId, @Valid @RequestBody TemplateSyncRequest request) {
    rateLimiter.assertWithinQuota(tenantId, "template-sync");
    auditLogger.logTenantAction(tenantId, "TEMPLATE_SYNC", request.versionTag());
    service.requestSync(tenantId, request);
  }
}
