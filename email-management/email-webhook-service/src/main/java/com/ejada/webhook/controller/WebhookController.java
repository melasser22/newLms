package com.ejada.webhook.controller;

import com.ejada.webhook.dto.SendGridWebhookRequest;
import com.ejada.webhook.service.SignatureVerifier;
import com.ejada.webhook.service.WebhookService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks/sendgrid")
public class WebhookController {

  private final WebhookService webhookService;
  private final SignatureVerifier signatureVerifier;
  private final ObjectMapper objectMapper;

  public WebhookController(
      WebhookService webhookService, SignatureVerifier signatureVerifier, ObjectMapper objectMapper) {
    this.webhookService = webhookService;
    this.signatureVerifier = signatureVerifier;
    this.objectMapper = objectMapper;
  }

  @PostMapping
  public ResponseEntity<Void> ingest(
      @RequestHeader("X-Timestamp") long timestamp,
      @RequestHeader("X-Signature") String signature,
      @RequestBody String payload) throws JsonProcessingException {
    if (!signatureVerifier.isValid(payload, signature, timestamp)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    SendGridWebhookRequest request = objectMapper.readValue(payload, SendGridWebhookRequest.class);
    webhookService.handle(request);
    return ResponseEntity.accepted().build();
  }
}
