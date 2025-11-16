package com.ejada.template.controller;

import com.ejada.template.service.WebhookService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import org.springframework.validation.annotation.Validated;

@Validated
@RestController
@RequestMapping("/api/v1/webhooks/sendgrid")
public class WebhookController {

  private final WebhookService webhookService;

  public WebhookController(WebhookService webhookService) {
    this.webhookService = webhookService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void handleWebhook(
      @RequestHeader("X-Tenant-Id") @NotBlank String tenantId,
      @RequestHeader("X-Twilio-Email-Event-Webhook-Signature") String signature,
      @RequestHeader("X-Twilio-Email-Event-Webhook-Timestamp") String timestamp,
      @RequestBody String payload) {
    webhookService.handleWebhook(tenantId, payload, signature, timestamp);
  }
}
