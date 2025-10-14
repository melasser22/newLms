package com.ejada.gateway.security.apikey;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

/**
 * Canonical representation of an API key stored in Redis.
 */
public class ApiKeyRecord {

  private String tenantId;
  private Set<String> scopes = Set.of();
  private Instant expiresAt;
  private Instant rotatedAt;
  private Instant lastUsedAt;
  private Long rateLimitPerMinute;
  private String label;
  private Instant createdAt;

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  public Set<String> getScopes() {
    return scopes == null ? Set.of() : scopes;
  }

  public void setScopes(Collection<String> scopes) {
    if (scopes == null || scopes.isEmpty()) {
      this.scopes = Set.of();
      return;
    }
    this.scopes = scopes.stream()
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(StringUtils::hasText)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  public Instant getRotatedAt() {
    return rotatedAt;
  }

  public void setRotatedAt(Instant rotatedAt) {
    this.rotatedAt = rotatedAt;
  }

  public Instant getLastUsedAt() {
    return lastUsedAt;
  }

  public void setLastUsedAt(Instant lastUsedAt) {
    this.lastUsedAt = lastUsedAt;
  }

  public Long getRateLimitPerMinute() {
    return rateLimitPerMinute;
  }

  public void setRateLimitPerMinute(Long rateLimitPerMinute) {
    this.rateLimitPerMinute = rateLimitPerMinute;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = StringUtils.hasText(label) ? label.trim() : null;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
