package com.ejada.email.sending.service;

public interface RateLimiterService {
  boolean tryConsume(String tenantId);
}
