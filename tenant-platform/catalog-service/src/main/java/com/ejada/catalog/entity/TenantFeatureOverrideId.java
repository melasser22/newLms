package com.ejada.catalog.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class TenantFeatureOverrideId implements Serializable {

    private UUID tenantId;
    private String featureKey;

    public TenantFeatureOverrideId() {
    }

    public TenantFeatureOverrideId(UUID tenantId, String featureKey) {
        this.tenantId = tenantId;
        this.featureKey = featureKey;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public String getFeatureKey() {
        return featureKey;
    }

    public void setFeatureKey(String featureKey) {
        this.featureKey = featureKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TenantFeatureOverrideId that = (TenantFeatureOverrideId) o;
        return Objects.equals(tenantId, that.tenantId) && Objects.equals(featureKey, that.featureKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenantId, featureKey);
    }
}
