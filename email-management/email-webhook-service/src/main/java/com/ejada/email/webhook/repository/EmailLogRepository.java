package com.ejada.email.webhook.repository;

import com.ejada.email.webhook.model.EmailLogStatus;
import java.util.Map;
import java.util.Optional;

public interface EmailLogRepository {
  void upsertStatus(String messageId, EmailLogStatus status, String tenantId);

  Optional<EmailLogStatus> findStatus(String messageId);

  Map<String, EmailLogStatus> findAll();
}
