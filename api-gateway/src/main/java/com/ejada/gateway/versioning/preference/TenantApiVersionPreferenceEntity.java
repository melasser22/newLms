package com.ejada.gateway.versioning.preference;

import com.ejada.gateway.versioning.VersionNumber;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.util.StringUtils;

@Table(value = "tenant_api_version_preferences", schema = "public")
public class TenantApiVersionPreferenceEntity {

  @Id
  private Long id;

  @Column("tenant_id")
  private String tenantId;

  private String resource;

  @Column("preferred_version")
  private String preferredVersion;

  @Column("fallback_version")
  private String fallbackVersion;

  @Column("updated_at")
  private Instant updatedAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId != null ? tenantId.trim() : null;
  }

  public String getResource() {
    return resource;
  }

  public void setResource(String resource) {
    if (!StringUtils.hasText(resource)) {
      this.resource = null;
      return;
    }
    this.resource = resource.trim().toLowerCase();
  }

  public String getPreferredVersion() {
    return preferredVersion;
  }

  public void setPreferredVersion(String preferredVersion) {
    this.preferredVersion = VersionNumber.canonicaliseOrNull(preferredVersion);
  }

  public String getFallbackVersion() {
    return fallbackVersion;
  }

  public void setFallbackVersion(String fallbackVersion) {
    this.fallbackVersion = VersionNumber.canonicaliseOrNull(fallbackVersion);
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
