package com.ejada.management.controller;

import com.ejada.management.dto.EmailSendRequest;
import com.ejada.management.dto.EmailSendResponse;
import com.ejada.management.service.EmailGatewayService;
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

  public EmailGatewayController(EmailGatewayService service) {
    this.service = service;
  }

  @PostMapping("/send")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public EmailSendResponse sendEmail(
      @PathVariable String tenantId, @Valid @RequestBody EmailSendRequest request) {
    return service.sendEmail(tenantId, request);
  }
}
