package com.ejada.email.webhook.repository;

import com.ejada.email.webhook.model.EmailEvent;
import com.ejada.email.webhook.model.EmailEventType;
import java.util.List;
import java.util.Optional;

public interface EmailEventRepository {
  EmailEvent save(EmailEvent event);

  Optional<EmailEvent> findByEventId(String eventId);

  List<EmailEvent> findAll();

  List<EmailEvent> findByType(EmailEventType type);
}
