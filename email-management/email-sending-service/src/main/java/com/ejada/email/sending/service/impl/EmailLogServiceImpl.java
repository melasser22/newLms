package com.ejada.email.sending.service.impl;

import com.ejada.email.sending.messaging.EmailEnvelope;
import com.ejada.email.sending.persistence.EmailLog;
import com.ejada.email.sending.persistence.EmailLogRepository;
import com.ejada.email.sending.persistence.EmailStatus;
import com.ejada.email.sending.service.EmailLogService;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailLogServiceImpl implements EmailLogService {

  private static final Logger log = LoggerFactory.getLogger(EmailLogServiceImpl.class);

  private final EmailLogRepository repository;

  public EmailLogServiceImpl(EmailLogRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public void recordQueued(EmailEnvelope envelope) {
    EmailLog logEntry = new EmailLog();
    logEntry.setId(envelope.id());
    logEntry.setTenantId(envelope.tenantId());
    logEntry.setTemplateKey(envelope.templateKey());
    logEntry.setMode(envelope.mode().name());
    logEntry.setToRecipients(String.join(",", envelope.to()));
    logEntry.setCcRecipients(envelope.cc() == null ? null : String.join(",", envelope.cc()));
    logEntry.setBccRecipients(envelope.bcc() == null ? null : String.join(",", envelope.bcc()));
    logEntry.setStatus(EmailStatus.QUEUED);
    logEntry.setAttemptCount(0);
    logEntry.setIdempotencyKey(envelope.idempotencyKey());
    repository.save(logEntry);
  }

  @Override
  @Transactional
  public void markSent(String id) {
    updateStatus(id, EmailStatus.SENT, null);
  }

  @Override
  @Transactional
  public void markTestSent(String id) {
    updateStatus(id, EmailStatus.SKIPPED_TEST, null);
  }

  @Override
  @Transactional
  public void markDraftSkipped(String id) {
    updateStatus(id, EmailStatus.SKIPPED_DRAFT, null);
  }

  @Override
  @Transactional
  public void markFailed(String id, String errorMessage) {
    updateStatus(id, EmailStatus.FAILED, errorMessage);
  }

  @Override
  @Transactional
  public void markDeadLetter(String id, String errorMessage) {
    updateStatus(id, EmailStatus.DEAD_LETTER, errorMessage);
  }

  @Override
  @Transactional
  public void markRateLimited(String tenantId) {
    log.info("Rate limit exceeded for tenant {}", tenantId);
  }

  @Override
  @Transactional
  public void markDuplicate(String tenantId, String idempotencyKey) {
    log.info("Duplicate send detected for tenant {} with key {}", tenantId, idempotencyKey);
  }

  @Override
  @Transactional
  public void incrementAttempts(String id) {
    Optional<EmailLog> maybeLog = repository.findById(id);
    maybeLog.ifPresent(
        entry -> {
          entry.setAttemptCount(entry.getAttemptCount() + 1);
          repository.save(entry);
        });
  }

  private void updateStatus(String id, EmailStatus status, String errorMessage) {
    Optional<EmailLog> maybeLog = repository.findById(id);
    if (maybeLog.isEmpty()) {
      log.warn("Email log {} not found for status update to {}", id, status);
      return;
    }
    EmailLog emailLog = maybeLog.get();
    emailLog.setStatus(status);
    emailLog.setLastError(errorMessage);
    repository.save(emailLog);
  }
}
