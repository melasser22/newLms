package com.ejada.starter_data.config;

import com.ejada.common.BaseStarterProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("shared.data.tenant-filter")
public class StarterDataTenantProps implements BaseStarterProperties {
    /** Enable tenant filter aspect */
    private boolean enabled = true;
    /** Default include-global if no annotation present */
    private boolean defaultIncludeGlobal = false;
    /** Hibernate filter name */
    private String filterName = "tenantFilter";
    /** Param names (must match @FilterDef) */
    private String tenantIdParam = "tenantId";
    private String allowGlobalParam = "allowGlobal";
}
