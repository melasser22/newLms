package com.ejada.shared_starter_ratelimit;

import com.ejada.shared_starter_ratelimit.RateLimitProps.Dimension;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Generates deterministic Redis keys for a given strategy and request context.
 */
public class RateLimitKeyGenerator {

  private static final HexFormat HEX = HexFormat.of();

  public String buildKey(RateLimitStrategy strategy, RateLimitEvaluationRequest request, RateLimitTier tier) {
    StringBuilder raw = new StringBuilder();
    raw.append(strategy.name()).append('|').append(tier.name());
    for (Dimension dimension : strategy.dimensions()) {
      raw.append('|');
      switch (dimension) {
        case TENANT -> raw.append(request.safeTenantId());
        case USER -> raw.append(request.safeUserId());
        case IP -> raw.append(request.safeIpAddress());
        case ENDPOINT -> raw.append(request.safeEndpoint());
        default -> raw.append("unknown");
      }
    }
    return "rl:" + strategy.name() + ':' + digest(raw.toString());
  }

  private String digest(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
      return HEX.formatHex(hash).substring(0, 32);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }
}
