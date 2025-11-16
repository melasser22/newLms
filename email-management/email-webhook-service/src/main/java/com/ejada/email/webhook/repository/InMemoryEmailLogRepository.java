package com.ejada.email.webhook.repository;

import com.ejada.email.webhook.model.EmailLogStatus;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryEmailLogRepository implements EmailLogRepository {

  private final Map<String, EmailLogStatus> statusByMessageId = new ConcurrentHashMap<>();
  private final Map<String, String> tenantByMessageId = new ConcurrentHashMap<>();

  @Override
  public void upsertStatus(String messageId, EmailLogStatus status, String tenantId) {
    if (messageId == null || messageId.isEmpty()) {
      return;
    }
    statusByMessageId.put(messageId, status);
    if (tenantId != null) {
      tenantByMessageId.put(messageId, tenantId);
    }
  }

  @Override
  public Optional<EmailLogStatus> findStatus(String messageId) {
    return Optional.ofNullable(statusByMessageId.get(messageId));
  }

  @Override
  public Map<String, EmailLogStatus> findAll() {
    return Map.copyOf(statusByMessageId);
  }

  public Map<String, String> tenants() {
    return Map.copyOf(tenantByMessageId);
  }
}
