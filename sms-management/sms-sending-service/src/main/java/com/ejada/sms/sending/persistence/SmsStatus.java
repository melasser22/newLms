package com.ejada.sms.sending.persistence;

public enum SmsStatus {
  QUEUED,
  SENT,
  FAILED,
  RATE_LIMITED,
  DUPLICATE
}
