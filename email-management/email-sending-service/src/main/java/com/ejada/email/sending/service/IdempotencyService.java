package com.ejada.email.sending.service;

public interface IdempotencyService {
  boolean register(String tenantId, String key, String value);
}
