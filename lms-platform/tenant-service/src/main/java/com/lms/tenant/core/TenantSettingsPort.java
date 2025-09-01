package com.lms.tenant.core;

import java.util.UUID;

public interface TenantSettingsPort {
    boolean isOverageEnabled(UUID tenantId);
    void setOverageEnabled(UUID tenantId, boolean enabled);
}
