package com.lms.catalog.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "tenant_feature_override")
public class TenantFeatureOverrideEntity {

    @EmbeddedId
    private TenantFeatureOverrideId id;

    @Column(name = "enabled")
    private Boolean enabled;

    @Column(name = "limit_value")
    private Long limitValue;

    @Column(name = "allow_overage_override")
    private Boolean allowOverageOverride;

    @Column(name = "overage_unit_price_minor_override")
    private Long overageUnitPriceMinorOverride;

    @Column(name = "overage_currency_override")
    private String overageCurrencyOverride;

    public TenantFeatureOverrideId getId() {
        return id;
    }

    public void setId(TenantFeatureOverrideId id) {
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

    public Boolean getAllowOverageOverride() {
        return allowOverageOverride;
    }

    public void setAllowOverageOverride(Boolean allowOverageOverride) {
        this.allowOverageOverride = allowOverageOverride;
    }

    public Long getOverageUnitPriceMinorOverride() {
        return overageUnitPriceMinorOverride;
    }

    public void setOverageUnitPriceMinorOverride(Long overageUnitPriceMinorOverride) {
        this.overageUnitPriceMinorOverride = overageUnitPriceMinorOverride;
    }

    public String getOverageCurrencyOverride() {
        return overageCurrencyOverride;
    }

    public void setOverageCurrencyOverride(String overageCurrencyOverride) {
        this.overageCurrencyOverride = overageCurrencyOverride;
    }
}
