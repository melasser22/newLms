package com.ejada.email.sending.controller;

import com.ejada.common.context.ContextManager;
import com.ejada.common.exception.ValidationException;
import com.ejada.email.sending.dto.BulkEmailSendRequest;
import com.ejada.email.sending.dto.EmailSendRequest;
import com.ejada.email.sending.dto.EmailSendResponse;
import com.ejada.email.sending.service.EmailDispatchService;
import com.ejada.starter_core.tenant.RequireTenant;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/emails")
@RequireTenant
public class EmailSendController {

  private final EmailDispatchService service;

  public EmailSendController(EmailDispatchService service) {
    this.service = service;
  }

  @PostMapping("/send")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public EmailSendResponse send(
      @Valid @RequestBody EmailSendRequest request) {
    String tenantId = requireTenantId();
    return service.sendEmail(tenantId, request);
  }

  @PostMapping("/send/bulk")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void bulkSend(
      @Valid @RequestBody BulkEmailSendRequest request) {
    String tenantId = requireTenantId();
    service.sendBulk(tenantId, request);
  }

  private String requireTenantId() {
    String tenantId = ContextManager.Tenant.get();
    if (tenantId == null) {
      throw new ValidationException("Tenant context is missing", "tenantId is required");
    }
    return tenantId;
  }
}
