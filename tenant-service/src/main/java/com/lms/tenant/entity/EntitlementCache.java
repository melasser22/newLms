package com.lms.tenant.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "entitlement_cache")
public class EntitlementCache {
    @Id
    private UUID tenantId;

    private Instant snapshot;

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public Instant getSnapshot() { return snapshot; }
    public void setSnapshot(Instant snapshot) { this.snapshot = snapshot; }
}
