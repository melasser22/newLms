package com.ejada.management.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

public record SendGridWebhookRequest(@NotEmpty List<Map<String, Object>> events) {

  public SendGridWebhookRequest {
    events = List.copyOf(events);
  }
}
