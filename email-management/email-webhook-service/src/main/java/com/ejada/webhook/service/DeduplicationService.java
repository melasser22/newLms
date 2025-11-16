package com.ejada.webhook.service;

public interface DeduplicationService {
  boolean seen(String messageId);
}
