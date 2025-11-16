package com.ejada.email.webhook.service;

import com.ejada.email.webhook.SendgridWebhookProperties;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class SignatureValidator {

  private static final String HMAC_SHA256 = "HmacSHA256";

  private final SendgridWebhookProperties properties;

  public SignatureValidator(SendgridWebhookProperties properties) {
    this.properties = properties;
  }

  public boolean isSignatureValid(String timestamp, String signature, String payload) {
    if (timestamp == null || signature == null || payload == null) {
      return false;
    }
    if (!isTimestampFresh(timestamp)) {
      return false;
    }
    try {
      Mac mac = Mac.getInstance(HMAC_SHA256);
      mac.init(new SecretKeySpec(properties.getSigningSecret().getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
      byte[] expected = mac.doFinal((timestamp + payload).getBytes(StandardCharsets.UTF_8));
      String expectedSignature = Base64.getEncoder().encodeToString(expected);
      return constantTimeEquals(expectedSignature, signature);
    } catch (Exception e) {
      return false;
    }
  }

  private boolean isTimestampFresh(String timestamp) {
    long ts = Long.parseLong(timestamp);
    long now = Instant.now().getEpochSecond();
    return Math.abs(now - ts) <= properties.getToleranceSeconds();
  }

  private boolean constantTimeEquals(String a, String b) {
    if (a == null || b == null || a.length() != b.length()) {
      return false;
    }
    int result = 0;
    for (int i = 0; i < a.length(); i++) {
      result |= a.charAt(i) ^ b.charAt(i);
    }
    return result == 0;
  }
}
