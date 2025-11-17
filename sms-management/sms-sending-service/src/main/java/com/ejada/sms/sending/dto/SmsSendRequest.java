package com.ejada.sms.sending.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record SmsSendRequest(
    @NotBlank String recipient,
    String senderId,
    String templateCode,
    String locale,
    @NotBlank String message,
    Map<String, String> variables,
    String idempotencyKey,
    String clientReference
) {}
