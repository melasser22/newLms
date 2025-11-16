package com.ejada.email.webhook.model;

public enum EmailLogStatus {
  PENDING,
  DELIVERED,
  BOUNCED,
  DROPPED,
  DEFERRED,
  SPAM_REPORTED,
  UNSUBSCRIBED,
  UNKNOWN
}
