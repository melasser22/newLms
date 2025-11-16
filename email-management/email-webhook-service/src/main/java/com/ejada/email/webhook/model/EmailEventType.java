package com.ejada.email.webhook.model;

public enum EmailEventType {
  DELIVERED,
  BOUNCED,
  SPAM_REPORTED,
  OPENED,
  CLICKED,
  UNSUBSCRIBED,
  GROUP_UNSUBSCRIBED,
  DEFERRED,
  DROPPED,
  PROCESSED,
  UNKNOWN
}
