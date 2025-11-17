package com.ejada.sms.sending.service;

import com.ejada.sms.sending.messaging.SmsEnvelope;

public interface SmsLogService {
  void recordQueued(SmsEnvelope envelope);

  void markRateLimited(String tenantId);

  void markDuplicate(String tenantId, String idempotencyKey);
}
