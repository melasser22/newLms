package com.ejada.sms.sending.service;

public interface RateLimiterService {
  boolean tryConsume(String tenantId);
}
