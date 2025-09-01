package com.lms.tenant.adapter;

import com.lms.tenant.core.TenantSettingsPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@ConditionalOnClass(name = "com.shared.tenant.SharedTenantSettingsClient")
public class SharedTenantSettingsAdapter implements TenantSettingsPort {
    @Override
    public boolean isOverageEnabled(UUID tenantId) {
        return true;
    }

    @Override
    public void setOverageEnabled(UUID tenantId, boolean enabled) {
        // no-op for test stub
    }
}
