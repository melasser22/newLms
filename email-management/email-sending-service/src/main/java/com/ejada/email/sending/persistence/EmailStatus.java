package com.ejada.email.sending.persistence;

public enum EmailStatus {
  QUEUED,
  SENT,
  FAILED,
  DEAD_LETTER,
  SKIPPED_TEST,
  SKIPPED_DRAFT,
  RATE_LIMITED,
  DUPLICATE
}
