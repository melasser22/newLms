package com.ejada.email.webhook.controller;

import com.ejada.email.webhook.model.EmailEvent;
import com.ejada.email.webhook.model.SendgridEventRequest;
import com.ejada.email.webhook.service.IpWhitelistService;
import com.ejada.email.webhook.service.SendgridEventProcessor;
import com.ejada.email.webhook.service.SignatureValidator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks/sendgrid")
public class WebhookController {

  private static final Logger log = LoggerFactory.getLogger(WebhookController.class);
  private static final String SIGNATURE_HEADER = "X-Twilio-Email-Event-Webhook-Signature";
  private static final String TIMESTAMP_HEADER = "X-Twilio-Email-Event-Webhook-Timestamp";

  private final SignatureValidator signatureValidator;
  private final SendgridEventProcessor processor;
  private final IpWhitelistService ipWhitelistService;
  private final ObjectMapper objectMapper;

  public WebhookController(
      SignatureValidator signatureValidator,
      SendgridEventProcessor processor,
      IpWhitelistService ipWhitelistService,
      ObjectMapper objectMapper) {
    this.signatureValidator = Objects.requireNonNull(signatureValidator);
    this.processor = Objects.requireNonNull(processor);
    this.ipWhitelistService = Objects.requireNonNull(ipWhitelistService);
    this.objectMapper = Objects.requireNonNull(objectMapper).copy();
  }

  @PostMapping
  public ResponseEntity<List<EmailEvent>> receive(
      @RequestHeader(name = SIGNATURE_HEADER) String signature,
      @RequestHeader(name = TIMESTAMP_HEADER) String timestamp,
      @RequestBody String payload,
      HttpServletRequest request) {
    if (!ipWhitelistService.isAllowed(request.getRemoteAddr())) {
      log.warn("Rejected webhook from non-whitelisted IP: {}", request.getRemoteAddr());
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    if (!signatureValidator.isSignatureValid(timestamp, signature, payload)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    try {
      List<SendgridEventRequest> events =
          objectMapper.readValue(payload, new TypeReference<List<SendgridEventRequest>>() {});
      List<EmailEvent> processed = events.stream().map(processor::process).toList();
      return ResponseEntity.accepted().body(processed);
    } catch (IOException ex) {
      log.error("Unable to parse webhook payload", ex);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
  }
}
