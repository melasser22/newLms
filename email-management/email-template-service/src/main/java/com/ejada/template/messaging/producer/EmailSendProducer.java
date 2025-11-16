package com.ejada.template.messaging.producer;

import com.ejada.template.config.KafkaTopicsProperties;
import com.ejada.template.messaging.model.EmailSendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailSendProducer {

  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final KafkaTopicsProperties topicsProperties;

  public void publish(EmailSendMessage message) {
    kafkaTemplate.send(topicsProperties.emailSend(), message.getSendId().toString(), message);
    log.info("Published email send {} to Kafka", message.getSendId());
  }
}
