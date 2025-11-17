package com.ejada.email.template.messaging.producer;

import com.ejada.email.template.config.KafkaTopicsProperties;
import com.ejada.email.template.messaging.model.WebhookEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookEventProducer {

  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final KafkaTopicsProperties topicsProperties;

  public void publish(WebhookEventMessage message) {
    kafkaTemplate.send(topicsProperties.webhookEvents(), message.getTenantId(), message);
    log.info("Published {} webhook events for tenant {}", message.getEvents().size(), message.getTenantId());
  }
}
