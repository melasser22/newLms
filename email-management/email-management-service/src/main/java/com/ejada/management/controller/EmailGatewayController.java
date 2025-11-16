package com.ejada.management.controller;

import com.ejada.management.dto.EmailSendRequest;
import com.ejada.management.dto.EmailSendResponse;
import com.ejada.management.service.AuditLogger;
import com.ejada.management.service.EmailGatewayService;
import com.ejada.management.service.TenantRateLimiter;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/emails")
public class EmailGatewayController {

  private final EmailGatewayService service;
  private final TenantRateLimiter rateLimiter;
  private final AuditLogger auditLogger;

  public EmailGatewayController(
      EmailGatewayService service, TenantRateLimiter rateLimiter, AuditLogger auditLogger) {
    this.service = service;
    this.rateLimiter = rateLimiter;
    this.auditLogger = auditLogger;
  }

  @PostMapping("/send")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public EmailSendResponse sendEmail(
      @PathVariable String tenantId, @Valid @RequestBody EmailSendRequest request) {
    rateLimiter.assertWithinQuota(tenantId, "email-send");
    auditLogger.logTenantAction(tenantId, "EMAIL_SEND", request.templateKey());
    return service.sendEmail(tenantId, request);
  }
}
