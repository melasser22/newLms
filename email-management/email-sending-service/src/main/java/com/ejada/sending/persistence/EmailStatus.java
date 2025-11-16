package com.ejada.sending.persistence;

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
