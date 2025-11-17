package com.ejada.email.sending.controller;

import com.ejada.email.sending.dto.BulkEmailSendRequest;
import com.ejada.email.sending.dto.EmailSendRequest;
import com.ejada.email.sending.dto.EmailSendResponse;
import com.ejada.email.sending.service.EmailDispatchService;
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
public class EmailSendController {

  private final EmailDispatchService service;

  public EmailSendController(EmailDispatchService service) {
    this.service = service;
  }

  @PostMapping("/send")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public EmailSendResponse send(
      @PathVariable String tenantId, @Valid @RequestBody EmailSendRequest request) {
    return service.sendEmail(tenantId, request);
  }

  @PostMapping("/send/bulk")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void bulkSend(
      @PathVariable String tenantId, @Valid @RequestBody BulkEmailSendRequest request) {
    service.sendBulk(tenantId, request);
  }
}
