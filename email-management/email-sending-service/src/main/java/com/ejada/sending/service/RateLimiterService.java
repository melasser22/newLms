package com.ejada.sending.service;

public interface RateLimiterService {
  boolean tryConsume(String tenantId);
}
