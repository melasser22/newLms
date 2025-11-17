package com.ejada.sms.sending.service.impl;

import com.ejada.sms.sending.config.KafkaTopicsProperties;
import com.ejada.sms.sending.dto.BulkSmsSendRequest;
import com.ejada.sms.sending.dto.SmsSendRequest;
import com.ejada.sms.sending.dto.SmsSendResponse;
import com.ejada.sms.sending.messaging.SmsEnvelope;
import com.ejada.sms.sending.service.IdempotencyService;
import com.ejada.sms.sending.service.RateLimiterService;
import com.ejada.sms.sending.service.SmsDispatchService;
import com.ejada.sms.sending.service.SmsLogService;
import java.util.concurrent.CompletableFuture;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class SmsDispatchServiceImpl implements SmsDispatchService {

  private final KafkaTemplate<String, SmsEnvelope> kafkaTemplate;
  private final KafkaTopicsProperties topics;
  private final IdempotencyService idempotencyService;
  private final RateLimiterService rateLimiterService;
  private final SmsLogService smsLogService;

  public SmsDispatchServiceImpl(
      KafkaTemplate<String, SmsEnvelope> kafkaTemplate,
      KafkaTopicsProperties topics,
      IdempotencyService idempotencyService,
      RateLimiterService rateLimiterService,
      SmsLogService smsLogService) {
    this.kafkaTemplate = kafkaTemplate;
    this.topics = topics;
    this.idempotencyService = idempotencyService;
    this.rateLimiterService = rateLimiterService;
    this.smsLogService = smsLogService;
  }

  @Override
  public SmsSendResponse send(String tenantId, SmsSendRequest request) {
    Assert.hasText(tenantId, "tenantId is required");
    if (!rateLimiterService.tryConsume(tenantId)) {
      smsLogService.markRateLimited(tenantId);
      return new SmsSendResponse(null, "RATE_LIMITED");
    }

    if (request.idempotencyKey() != null
        && !idempotencyService.register(tenantId, request.idempotencyKey(), "queued")) {
      smsLogService.markDuplicate(tenantId, request.idempotencyKey());
      return new SmsSendResponse(null, "DUPLICATE");
    }

    SmsEnvelope envelope = SmsEnvelope.from(tenantId, request);
    smsLogService.recordQueued(envelope);
    kafkaTemplate.send(buildMessage(topics.getSmsSend(), tenantId, envelope, 1));
    return new SmsSendResponse(envelope.id(), "QUEUED");
  }

  @Override
  public void sendBulk(String tenantId, BulkSmsSendRequest request) {
    request.entries().forEach(entry -> {
      if (!rateLimiterService.tryConsume(tenantId)) {
        smsLogService.markRateLimited(tenantId);
        return;
      }
      if (entry.idempotencyKey() != null
          && !idempotencyService.register(tenantId, entry.idempotencyKey(), "queued")) {
        smsLogService.markDuplicate(tenantId, entry.idempotencyKey());
        return;
      }
      SmsEnvelope envelope = SmsEnvelope.from(tenantId, entry);
      smsLogService.recordQueued(envelope);
      CompletableFuture.runAsync(
          () -> kafkaTemplate.send(buildMessage(topics.getSmsBulk(), tenantId, envelope, 1)));
    });
  }

  private Message<SmsEnvelope> buildMessage(String topic, String tenantId, SmsEnvelope envelope, int attempt) {
    return MessageBuilder.withPayload(envelope)
        .setHeader(KafkaHeaders.TOPIC, topic)
        .setHeader(KafkaHeaders.KEY, tenantId)
        .setHeader("x-attempt", attempt)
        .build();
  }
}
