package com.ejada.push.sending.service;

import com.ejada.push.sending.model.SendLog;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class SendService {

  private static final Logger log = LoggerFactory.getLogger(SendService.class);

  private final List<SendLog> logs = new CopyOnWriteArrayList<>();
  private final KafkaTemplate<String, Object> kafkaTemplate;

  public SendService(@Autowired(required = false) KafkaTemplate<String, Object> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  public SendResponse enqueue(String tenantId, SendRequest request) {
    String requestId = UUID.randomUUID().toString();
    SendLog logEntry =
        new SendLog(requestId, tenantId, request.tokens(), request.templateKey(), request.variables(), "ENQUEUED");
    logs.add(logEntry);

    if (kafkaTemplate != null) {
      try {
        kafkaTemplate.send("push.request", requestId, request).get();
      } catch (Exception ex) {
        log.warn("Failed to publish to Kafka, continuing with in-memory log", ex);
      }
    }

    return new SendResponse(requestId, "ENQUEUED");
  }

  public List<SendLog> list(String tenantId) {
    return logs.stream().filter(entry -> entry.getTenantId().equals(tenantId)).toList();
  }
}
