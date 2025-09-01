package com.lms.catalog.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "tier_feature_limit")
public class TierFeatureLimitEntity {

    @EmbeddedId
    private TierFeatureLimitId id;

    @Column(name = "enabled")
    private Boolean enabled;

    @Column(name = "limit_value")
    private Long limitValue;

    @Column(name = "allow_overage")
    private Boolean allowOverage;

    @Column(name = "overage_unit_price_minor")
    private Long overageUnitPriceMinor;

    @Column(name = "overage_currency")
    private String overageCurrency;

    public TierFeatureLimitId getId() {
        return id;
    }

    public void setId(TierFeatureLimitId id) {
        this.id = id;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Long getLimitValue() {
        return limitValue;
    }

    public void setLimitValue(Long limitValue) {
        this.limitValue = limitValue;
    }

    public Boolean getAllowOverage() {
        return allowOverage;
    }

    public void setAllowOverage(Boolean allowOverage) {
        this.allowOverage = allowOverage;
    }

    public Long getOverageUnitPriceMinor() {
        return overageUnitPriceMinor;
    }

    public void setOverageUnitPriceMinor(Long overageUnitPriceMinor) {
        this.overageUnitPriceMinor = overageUnitPriceMinor;
    }

    public String getOverageCurrency() {
        return overageCurrency;
    }

    public void setOverageCurrency(String overageCurrency) {
        this.overageCurrency = overageCurrency;
    }
}
