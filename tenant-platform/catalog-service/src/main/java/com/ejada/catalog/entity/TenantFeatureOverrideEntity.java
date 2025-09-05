package com.ejada.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
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
}
