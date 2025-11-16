package com.ejada.webhook.service.impl;

import com.ejada.webhook.config.SendGridWebhookProperties;
import com.ejada.webhook.service.SignatureVerifier;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class HmacSignatureVerifier implements SignatureVerifier {

  private final SendGridWebhookProperties properties;

  public HmacSignatureVerifier(SendGridWebhookProperties properties) {
    this.properties = properties;
  }

  @Override
  public boolean isValid(String payload, String signature, long timestamp) {
    Assert.hasText(signature, "signature required");
    long toleranceSeconds = properties.getToleranceSeconds();
    long now = System.currentTimeMillis() / 1000;
    if (Math.abs(now - timestamp) > toleranceSeconds) {
      return false;
    }
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(properties.getSigningSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] digest = mac.doFinal((timestamp + payload).getBytes(StandardCharsets.UTF_8));
      String expected = bytesToHex(digest);
      return expected.equalsIgnoreCase(signature);
    } catch (GeneralSecurityException ex) {
      return false;
    }
  }

  private static String bytesToHex(byte[] bytes) {
    StringBuilder builder = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      builder.append(String.format("%02x", b));
    }
    return builder.toString();
  }
}
