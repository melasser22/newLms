package com.shared.starter_data.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("shared.data.tenant-filter")
public class StarterDataTenantProps {
    /** Enable tenant filter aspect */
    private boolean enabled = true;
    /** Default include-global if no annotation present */
    private boolean defaultIncludeGlobal = false;
    /** Hibernate filter name */
    private String filterName = "tenantFilter";
    /** Param names (must match @FilterDef) */
    private String tenantIdParam = "tenantId";
    private String allowGlobalParam = "allowGlobal";

    // getters/setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isDefaultIncludeGlobal() { return defaultIncludeGlobal; }
    public void setDefaultIncludeGlobal(boolean v) { this.defaultIncludeGlobal = v; }
    public String getFilterName() { return filterName; }
    public void setFilterName(String filterName) { this.filterName = filterName; }
    public String getTenantIdParam() { return tenantIdParam; }
    public void setTenantIdParam(String tenantIdParam) { this.tenantIdParam = tenantIdParam; }
    public String getAllowGlobalParam() { return allowGlobalParam; }
    public void setAllowGlobalParam(String allowGlobalParam) { this.allowGlobalParam = allowGlobalParam; }
}
