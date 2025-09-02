package com.ejada.catalog.entity;

import java.io.Serializable;
import java.util.Objects;

public class TierFeatureLimitId implements Serializable {

    private String tierId;
    private String featureKey;

    public TierFeatureLimitId() {
    }

    public TierFeatureLimitId(String tierId, String featureKey) {
        this.tierId = tierId;
        this.featureKey = featureKey;
    }

    public String getTierId() {
        return tierId;
    }

    public void setTierId(String tierId) {
        this.tierId = tierId;
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
        TierFeatureLimitId that = (TierFeatureLimitId) o;
        return Objects.equals(tierId, that.tierId) && Objects.equals(featureKey, that.featureKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tierId, featureKey);
    }
}
