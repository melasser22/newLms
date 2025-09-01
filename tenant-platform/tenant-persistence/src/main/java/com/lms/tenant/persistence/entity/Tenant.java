package com.lms.tenant.persistence.entity;

import com.lms.tenant.persistence.entity.enums.TenantStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Tenant aggregate root.
 */
@Entity
@Table(name = "tenant")
public class Tenant {

    @Id
    @Column(nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantStatus status;

    @Column(name = "overage_enabled", nullable = false)
    private boolean overageEnabled = false;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public TenantStatus getStatus() {
        return status;
    }

    public void setStatus(TenantStatus status) {
        this.status = status;
    }

    public boolean isOverageEnabled() {
        return overageEnabled;
    }

    public void setOverageEnabled(boolean overageEnabled) {
        this.overageEnabled = overageEnabled;
    }
}

