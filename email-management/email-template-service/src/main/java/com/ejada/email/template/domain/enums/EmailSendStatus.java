package com.ejada.template.domain.enums;

public enum EmailSendStatus {
  QUEUED,
  DISPATCHED,
  SENT,
  FAILED,
  RETRYING,
  DEAD_LETTERED,
  TESTED
}
