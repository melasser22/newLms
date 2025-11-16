package com.ejada.sending.service.impl;

import com.ejada.sending.config.KafkaTopicsProperties;
import com.ejada.sending.dto.BulkEmailSendRequest;
import com.ejada.sending.dto.EmailSendRequest;
import com.ejada.sending.dto.EmailSendResponse;
import com.ejada.sending.messaging.EmailEnvelope;
import com.ejada.sending.service.EmailDispatchService;
import com.ejada.sending.service.IdempotencyService;
import com.ejada.sending.service.RateLimiterService;
import java.util.concurrent.CompletableFuture;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class EmailDispatchServiceImpl implements EmailDispatchService {

  private final KafkaTemplate<String, EmailEnvelope> kafkaTemplate;
  private final KafkaTopicsProperties topics;
  private final IdempotencyService idempotencyService;
  private final RateLimiterService rateLimiterService;

  public EmailDispatchServiceImpl(
      KafkaTemplate<String, EmailEnvelope> kafkaTemplate,
      KafkaTopicsProperties topics,
      IdempotencyService idempotencyService,
      RateLimiterService rateLimiterService) {
    this.kafkaTemplate = kafkaTemplate;
    this.topics = topics;
    this.idempotencyService = idempotencyService;
    this.rateLimiterService = rateLimiterService;
  }

  @Override
  public EmailSendResponse sendEmail(String tenantId, EmailSendRequest request) {
    Assert.hasText(tenantId, "tenantId is required");
    if (!rateLimiterService.tryConsume(tenantId)) {
      return new EmailSendResponse(null, "RATE_LIMITED");
    }

    if (request.idempotencyKey() != null
        && !idempotencyService.register(tenantId, request.idempotencyKey(), "queued")) {
      return new EmailSendResponse(null, "DUPLICATE");
    }

    EmailEnvelope envelope = EmailEnvelope.from(tenantId, request);
    kafkaTemplate.send(topics.getEmailSend(), tenantId, envelope);
    return new EmailSendResponse(envelope.id(), "QUEUED");
  }

  @Override
  public void sendBulk(String tenantId, BulkEmailSendRequest request) {
    request.entries().forEach(entry -> {
      EmailEnvelope envelope = EmailEnvelope.from(tenantId, entry);
      CompletableFuture.runAsync(() -> kafkaTemplate.send(topics.getEmailBulk(), tenantId, envelope));
    });
  }
}
