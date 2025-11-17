package com.ejada.email.webhook.service;

import com.ejada.email.webhook.SendgridWebhookProperties;
import com.ejada.email.webhook.model.EmailEvent;
import com.ejada.email.webhook.model.EmailEventType;
import com.ejada.email.webhook.model.EmailLogStatus;
import com.ejada.email.webhook.model.SendgridEventRequest;
import com.ejada.email.webhook.repository.EmailEventRepository;
import com.ejada.email.webhook.repository.EmailLogRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP2",
    justification = "Dependencies are Spring-managed beans and not externally mutated")
public class SendgridEventProcessor {

  private static final Logger log = LoggerFactory.getLogger(SendgridEventProcessor.class);

  private final DeduplicationService deduplicationService;
  private final EventMapper mapper;
  private final EmailEventRepository emailEventRepository;
  private final EmailLogRepository emailLogRepository;
  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final SendgridWebhookProperties properties;
  private final ProcessingMetrics metrics;

  public SendgridEventProcessor(
      DeduplicationService deduplicationService,
      EventMapper mapper,
      EmailEventRepository emailEventRepository,
      EmailLogRepository emailLogRepository,
      KafkaTemplate<String, Object> kafkaTemplate,
      SendgridWebhookProperties properties,
      ProcessingMetrics metrics) {
    this.deduplicationService = deduplicationService;
    this.mapper = mapper;
    this.emailEventRepository = emailEventRepository;
    this.emailLogRepository = emailLogRepository;
    this.kafkaTemplate = kafkaTemplate;
    this.properties = properties;
    this.metrics = metrics;
  }

  public EmailEvent process(SendgridEventRequest request) {
    String eventId = deduplicationService.resolveEventId(request.getEventId(), request.getMessageId());
    boolean first = deduplicationService.markIfFirst(eventId);
    EmailEvent event = mapper.toEmailEvent(request, resolveTenantId(request), eventId, !first);
    emailEventRepository.save(event);

    EmailEventType type = event.getType();
    EmailLogStatus status = mapper.mapLogStatus(type);
    emailLogRepository.upsertStatus(event.getMessageId(), status, event.getTenantId());

    publish(event);

    metrics.incrementType(type.name());
    if (first) {
      metrics.incrementProcessed();
    } else {
      metrics.incrementDuplicate();
    }

    if (!first) {
      log.info("Duplicate SendGrid event suppressed: {}", eventId);
    }
    return event;
  }

  private void publish(EmailEvent event) {
    try {
      kafkaTemplate.send(properties.getKafkaTopic(), event.getEventId(), event);
    } catch (Exception ex) {
      metrics.incrementFailure();
      log.error("Failed to publish event {} to Kafka", event.getEventId(), ex);
    }
  }

  private String resolveTenantId(SendgridEventRequest request) {
    if (request.getCustomArgs() == null) {
      return null;
    }
    Object tenantId = request.getCustomArgs().getOrDefault("tenantId", request.getCustomArgs().get("tenant_id"));
    return tenantId != null ? tenantId.toString() : null;
  }

  public Map<String, Long> analytics() {
    Map<String, Long> counts = new HashMap<>();
    for (EmailEventType type : EmailEventType.values()) {
      counts.put(type.name(), (long) emailEventRepository.findByType(type).size());
    }
    return counts;
  }
}
