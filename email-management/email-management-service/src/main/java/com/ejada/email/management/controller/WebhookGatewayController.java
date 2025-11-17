package com.ejada.email.management.controller;

import com.ejada.email.management.dto.SendGridWebhookRequest;
import com.ejada.email.management.service.WebhookGatewayService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks/sendgrid")
public class WebhookGatewayController {

  private final WebhookGatewayService service;

  public WebhookGatewayController(WebhookGatewayService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void ingestWebhook(@Valid @RequestBody SendGridWebhookRequest request) {
    service.forwardWebhook(request);
  }
}
