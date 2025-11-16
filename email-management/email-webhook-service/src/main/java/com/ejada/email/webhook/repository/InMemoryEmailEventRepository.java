package com.ejada.email.webhook.repository;

import com.ejada.email.webhook.model.EmailEvent;
import com.ejada.email.webhook.model.EmailEventType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryEmailEventRepository implements EmailEventRepository {

  private final Map<String, EmailEvent> eventsByEventId = new ConcurrentHashMap<>();
  private final Map<String, EmailEvent> eventsById = new ConcurrentHashMap<>();

  @Override
  public EmailEvent save(EmailEvent event) {
    eventsByEventId.put(event.getEventId(), event);
    eventsById.put(event.getId().toString(), event);
    return event;
  }

  @Override
  public Optional<EmailEvent> findByEventId(String eventId) {
    return Optional.ofNullable(eventsByEventId.get(eventId));
  }

  @Override
  public List<EmailEvent> findAll() {
    return new ArrayList<>(eventsById.values());
  }

  @Override
  public List<EmailEvent> findByType(EmailEventType type) {
    return eventsById.values().stream().filter(event -> event.getType() == type).toList();
  }
}
