package com.lms.tenant.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_integration_key",
    uniqueConstraints = @UniqueConstraint(name="uq_key_prefix", columnNames = {"tenant_id","key_prefix"}))
public class TenantIntegrationKey {
  @Id @Column(name = "key_id", nullable = false) private UUID id;
  @Column(name = "tenant_id", nullable = false) private UUID tenantId;
  @Column(name = "key_prefix", nullable = false) private String keyPrefix;
  @Lob @Column(name = "key_hash", nullable = false) private byte[] keyHash;
  @Column(name = "name") private String name;
  @Column(name = "scopes", columnDefinition = "text[]") private String[] scopes;
  @Column(name = "rate_limit_per_min") private Integer rateLimitPerMin;
  @Column(name = "created_at", nullable = false) private Instant createdAt;
  @Column(name = "last_used_at") private Instant lastUsedAt;
  @Column(name = "expires_at") private Instant expiresAt;
  @Enumerated(EnumType.STRING) @Column(name = "status", nullable = false) private KeyStatus status = KeyStatus.ACTIVE;

  @PrePersist public void prePersist(){ if(id==null) id=UUID.randomUUID(); if(createdAt==null) createdAt=Instant.now(); }

  // getters/setters
  public UUID getId(){return id;} public void setId(UUID id){this.id=id;}
  public UUID getTenantId(){return tenantId;} public void setTenantId(UUID tenantId){this.tenantId=tenantId;}
  public String getKeyPrefix(){return keyPrefix;} public void setKeyPrefix(String keyPrefix){this.keyPrefix=keyPrefix;}
  public byte[] getKeyHash(){return keyHash;} public void setKeyHash(byte[] keyHash){this.keyHash=keyHash;}
  public String getName(){return name;} public void setName(String name){this.name=name;}
  public String[] getScopes(){return scopes;} public void setScopes(String[] scopes){this.scopes=scopes;}
  public Integer getRateLimitPerMin(){return rateLimitPerMin;} public void setRateLimitPerMin(Integer v){this.rateLimitPerMin=v;}
  public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant createdAt){this.createdAt=createdAt;}
  public Instant getLastUsedAt(){return lastUsedAt;} public void setLastUsedAt(Instant v){this.lastUsedAt=v;}
  public Instant getExpiresAt(){return expiresAt;} public void setExpiresAt(Instant v){this.expiresAt=v;}
  public KeyStatus getStatus(){return status;} public void setStatus(KeyStatus status){this.status=status;}
}
