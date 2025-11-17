package com.ejada.email.usage.domain;

public enum EmailEventType {
  PROCESSED,
  DELIVERED,
  DEFERRED,
  BOUNCED,
  DROPPED,
  OPENED,
  CLICKED,
  SPAM_COMPLAINT,
  BLOCKED;
}
