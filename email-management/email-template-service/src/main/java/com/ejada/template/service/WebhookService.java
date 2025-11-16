package com.ejada.template.service;

public interface WebhookService {
  void handleWebhook(String tenantId, String payload, String signature, String timestamp);
}
