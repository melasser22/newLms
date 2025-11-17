package com.ejada.sms.sending.service.impl;

import com.ejada.sms.sending.messaging.SmsEnvelope;
import com.ejada.sms.sending.persistence.SmsLog;
import com.ejada.sms.sending.persistence.SmsLogRepository;
import com.ejada.sms.sending.persistence.SmsStatus;
import com.ejada.sms.sending.service.SmsLogService;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SmsLogServiceImpl implements SmsLogService {

  private final SmsLogRepository repository;

  public SmsLogServiceImpl(SmsLogRepository repository) {
    this.repository = repository;
  }

  @Override
  public void recordQueued(SmsEnvelope envelope) {
    SmsLog log = new SmsLog();
    log.setId(envelope.id());
    log.setTenantId(envelope.tenantId());
    log.setRecipient(envelope.recipient());
    log.setSenderId(envelope.senderId());
    log.setTemplateCode(envelope.templateCode());
    log.setMessage(envelope.message());
    log.setClientReference(envelope.clientReference());
    log.setStatus(SmsStatus.QUEUED);
    log.setAttemptCount(0);
    repository.save(log);
  }

  @Override
  public void markRateLimited(String tenantId) {
    SmsLog log = new SmsLog();
    log.setId(UUID.randomUUID().toString());
    log.setTenantId(tenantId);
    log.setStatus(SmsStatus.RATE_LIMITED);
    log.setAttemptCount(0);
    repository.save(log);
  }

  @Override
  public void markDuplicate(String tenantId, String idempotencyKey) {
    SmsLog log = new SmsLog();
    log.setId(UUID.randomUUID().toString());
    log.setTenantId(tenantId);
    log.setIdempotencyKey(idempotencyKey);
    log.setStatus(SmsStatus.DUPLICATE);
    repository.save(log);
  }
}
