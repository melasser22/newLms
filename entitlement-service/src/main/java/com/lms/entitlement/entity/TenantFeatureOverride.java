package com.lms.entitlement.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "tenant_feature_override")
public class TenantFeatureOverride {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @ManyToOne(optional = false)
    @JoinColumn(name = "feature_key")
    private Feature feature;

    @Column(name = "feature_limit")
    private Long featureLimit;

    @Column(name = "allow_overage")
    private Boolean allowOverage;

    @Column(name = "overage_unit_price_minor")
    private Long overageUnitPriceMinor;

    @Column(name = "overage_currency")
    private String overageCurrency;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }
    public Feature getFeature() { return feature; }
    public void setFeature(Feature feature) { this.feature = feature; }
    public Long getFeatureLimit() { return featureLimit; }
    public void setFeatureLimit(Long featureLimit) { this.featureLimit = featureLimit; }
    public Boolean getAllowOverage() { return allowOverage; }
    public void setAllowOverage(Boolean allowOverage) { this.allowOverage = allowOverage; }
    public Long getOverageUnitPriceMinor() { return overageUnitPriceMinor; }
    public void setOverageUnitPriceMinor(Long overageUnitPriceMinor) { this.overageUnitPriceMinor = overageUnitPriceMinor; }
    public String getOverageCurrency() { return overageCurrency; }
    public void setOverageCurrency(String overageCurrency) { this.overageCurrency = overageCurrency; }
}
