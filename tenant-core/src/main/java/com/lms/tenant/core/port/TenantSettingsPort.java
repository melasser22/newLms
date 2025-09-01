package com.lms.tenant.core.port;

import java.util.UUID;

/** Port for tenant-level settings such as overage enablement. */
public interface TenantSettingsPort {
    boolean isOverageEnabled(UUID tenantId);
    void setOverageEnabled(UUID tenantId, boolean enabled);
}
