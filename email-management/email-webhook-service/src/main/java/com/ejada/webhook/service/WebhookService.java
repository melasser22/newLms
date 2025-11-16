package com.ejada.webhook.service;

import com.ejada.webhook.dto.SendGridWebhookRequest;

public interface WebhookService {
  void handle(SendGridWebhookRequest request);
}
