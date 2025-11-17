package com.ejada.email.sending.messaging;

import com.ejada.email.sending.config.EmailSendingProperties;
import com.ejada.email.sending.config.KafkaTopicsProperties;
import com.ejada.email.sending.service.EmailLogService;
import com.ejada.email.sending.service.EmailSender;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

@Component
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Dependencies are managed by Spring")
public class EmailSendConsumer {

  private static final Logger log = LoggerFactory.getLogger(EmailSendConsumer.class);

  private final EmailSender emailSender;
  private final EmailLogService emailLogService;
  private final KafkaTemplate<String, EmailEnvelope> kafkaTemplate;
  private final KafkaTopicsProperties topics;
  private final EmailSendingProperties properties;
  private final TaskScheduler taskScheduler;

  public EmailSendConsumer(
      EmailSender emailSender,
      EmailLogService emailLogService,
      KafkaTemplate<String, EmailEnvelope> kafkaTemplate,
      KafkaTopicsProperties topics,
      EmailSendingProperties properties,
      TaskScheduler taskScheduler) {
    this.emailSender = emailSender;
    this.emailLogService = emailLogService;
    this.kafkaTemplate = kafkaTemplate;
    this.topics = topics;
    this.properties = properties;
    this.taskScheduler = taskScheduler;
  }

  @KafkaListener(topics = {"${kafka.topics.email-send}", "${kafka.topics.email-bulk}"})
  public void consume(
      @Payload EmailEnvelope envelope,
      @Header(name = "x-attempt", required = false) Integer attemptHeader,
      @Header(name = KafkaHeaders.RECEIVED_TOPIC) String topic) {
    int attempt = attemptHeader == null ? 1 : attemptHeader;
    emailLogService.incrementAttempts(envelope.id());
    try {
      if (envelope.mode() == com.ejada.email.sending.dto.EmailSendRequest.SendMode.DRAFT) {
        emailLogService.markDraftSkipped(envelope.id());
        return;
      }
      emailSender.send(envelope);
      if (envelope.mode() == com.ejada.email.sending.dto.EmailSendRequest.SendMode.TEST) {
        emailLogService.markTestSent(envelope.id());
      } else {
        emailLogService.markSent(envelope.id());
      }
    } catch (Exception ex) {
      log.warn("Failed to process email {} on attempt {}", envelope.id(), attempt, ex);
      handleRetry(envelope, topic, attempt, ex);
    }
  }

  private void handleRetry(EmailEnvelope envelope, String topic, int attempt, Exception ex) {
    if (attempt >= properties.getMaxAttempts()) {
      sendWithAttempt(topics.getDeadLetter(), envelope, attempt + 1);
      emailLogService.markDeadLetter(envelope.id(), ex.getMessage());
      return;
    }

    long delayMillis = properties.getBackoffInitialMillis() * (long) Math.pow(2, attempt - 1);
    taskScheduler.schedule(() -> sendWithAttempt(topic, envelope, attempt + 1), Instant.now().plusMillis(delayMillis));
    emailLogService.markFailed(envelope.id(), ex.getMessage());
  }

  private void sendWithAttempt(String topic, EmailEnvelope envelope, int attempt) {
    kafkaTemplate.send(
        MessageBuilder.withPayload(envelope)
            .setHeader(KafkaHeaders.TOPIC, topic)
            .setHeader(KafkaHeaders.KEY, envelope.tenantId())
            .setHeader("x-attempt", attempt)
            .build());
  }
}
