package com.ejada.email.sending.service;

import com.ejada.email.sending.messaging.EmailEnvelope;

public interface EmailLogService {
  void recordQueued(EmailEnvelope envelope);

  void markSent(String id);

  void markTestSent(String id);

  void markDraftSkipped(String id);

  void markFailed(String id, String errorMessage);

  void markDeadLetter(String id, String errorMessage);

  void markRateLimited(String tenantId);

  void markDuplicate(String tenantId, String idempotencyKey);

  void incrementAttempts(String id);
}
