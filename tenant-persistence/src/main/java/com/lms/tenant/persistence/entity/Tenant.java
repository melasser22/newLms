package com.lms.tenant.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant", uniqueConstraints = @UniqueConstraint(name = "uq_tenant_slug", columnNames = "tenant_slug"))
public class Tenant {
  @Id
  @Column(name = "tenant_id", nullable = false)
  private UUID id;

  @Column(name = "tenant_slug", nullable = false)
  private String slug;

  @Column(name = "name", nullable = false)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private TenantStatus status = TenantStatus.ACTIVE;

  @Column(name = "tier_id")
  private String tierId;

  @Column(name = "timezone")
  private String timezone = "UTC";

  @Column(name = "locale")
  private String locale = "en";

  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(name = "domains", columnDefinition = "text[]")
  private String[] domains = new String[0];

  @Column(name = "overage_enabled", nullable = false)
  private boolean overageEnabled = false;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  public void prePersist() {
    if (id == null) id = UUID.randomUUID();
    if (createdAt == null) createdAt = Instant.now();
    if (updatedAt == null) updatedAt = createdAt;
  }

  @PreUpdate
  public void preUpdate() { this.updatedAt = Instant.now(); }

  // getters/setters
  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public String getSlug() { return slug; }
  public void setSlug(String slug) { this.slug = slug; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public TenantStatus getStatus() { return status; }
  public void setStatus(TenantStatus status) { this.status = status; }
  public String getTierId() { return tierId; }
  public void setTierId(String tierId) { this.tierId = tierId; }
  public String getTimezone() { return timezone; }
  public void setTimezone(String timezone) { this.timezone = timezone; }
  public String getLocale() { return locale; }
  public void setLocale(String locale) { this.locale = locale; }
  public String[] getDomains() { return domains; }
  public void setDomains(String[] domains) { this.domains = domains; }
  public boolean isOverageEnabled() { return overageEnabled; }
  public void setOverageEnabled(boolean overageEnabled) { this.overageEnabled = overageEnabled; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
