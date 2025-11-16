package com.ejada.webhook.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record SendGridWebhookRequest(@NotEmpty List<SendGridEvent> events) {}
