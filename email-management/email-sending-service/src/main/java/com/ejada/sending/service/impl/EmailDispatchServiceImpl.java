package com.ejada.sending.service.impl;

import com.ejada.sending.config.KafkaTopicsProperties;
import com.ejada.sending.dto.BulkEmailSendRequest;
import com.ejada.sending.dto.EmailSendRequest;
import com.ejada.sending.dto.EmailSendResponse;
import com.ejada.sending.messaging.EmailEnvelope;
import com.ejada.sending.service.EmailDispatchService;
import com.ejada.sending.service.IdempotencyService;
import com.ejada.sending.service.EmailLogService;
import com.ejada.sending.service.RateLimiterService;
import java.util.concurrent.CompletableFuture;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class EmailDispatchServiceImpl implements EmailDispatchService {

  private final KafkaTemplate<String, EmailEnvelope> kafkaTemplate;
  private final KafkaTopicsProperties topics;
  private final IdempotencyService idempotencyService;
  private final RateLimiterService rateLimiterService;
  private final EmailLogService emailLogService;

  public EmailDispatchServiceImpl(
      KafkaTemplate<String, EmailEnvelope> kafkaTemplate,
      KafkaTopicsProperties topics,
      IdempotencyService idempotencyService,
      RateLimiterService rateLimiterService,
      EmailLogService emailLogService) {
    this.kafkaTemplate = kafkaTemplate;
    this.topics = topics;
    this.idempotencyService = idempotencyService;
    this.rateLimiterService = rateLimiterService;
    this.emailLogService = emailLogService;
  }

  @Override
  public EmailSendResponse sendEmail(String tenantId, EmailSendRequest request) {
    Assert.hasText(tenantId, "tenantId is required");
    if (!rateLimiterService.tryConsume(tenantId)) {
      emailLogService.markRateLimited(tenantId);
      return new EmailSendResponse(null, "RATE_LIMITED");
    }

    if (request.idempotencyKey() != null
        && !idempotencyService.register(tenantId, request.idempotencyKey(), "queued")) {
      emailLogService.markDuplicate(tenantId, request.idempotencyKey());
      return new EmailSendResponse(null, "DUPLICATE");
    }

    EmailEnvelope envelope = EmailEnvelope.from(tenantId, request);
    emailLogService.recordQueued(envelope);
    kafkaTemplate.send(buildMessage(topics.getEmailSend(), tenantId, envelope, 1));
    return new EmailSendResponse(envelope.id(), "QUEUED");
  }

  @Override
  public void sendBulk(String tenantId, BulkEmailSendRequest request) {
    request.entries().forEach(entry -> {
      if (!rateLimiterService.tryConsume(tenantId)) {
        emailLogService.markRateLimited(tenantId);
        return;
      }
      if (entry.idempotencyKey() != null
          && !idempotencyService.register(tenantId, entry.idempotencyKey(), "queued")) {
        emailLogService.markDuplicate(tenantId, entry.idempotencyKey());
        return;
      }
      EmailEnvelope envelope = EmailEnvelope.from(tenantId, entry);
      emailLogService.recordQueued(envelope);
      CompletableFuture.runAsync(
          () -> kafkaTemplate.send(buildMessage(topics.getEmailBulk(), tenantId, envelope, 1)));
    });
  }

  private Message<EmailEnvelope> buildMessage(
      String topic, String tenantId, EmailEnvelope envelope, int attempt) {
    return MessageBuilder.withPayload(envelope)
        .setHeader(KafkaHeaders.TOPIC, topic)
        .setHeader(KafkaHeaders.KEY, tenantId)
        .setHeader("x-attempt", attempt)
        .build();
  }
}
