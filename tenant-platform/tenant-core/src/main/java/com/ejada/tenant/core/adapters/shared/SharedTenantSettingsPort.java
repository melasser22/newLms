package com.ejada.tenant.core.adapters.shared;

import com.ejada.tenant.core.TenantSettingsPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnClass(name = "com.shared.tenant.api.TenantService")
@ConditionalOnMissingBean(TenantSettingsPort.class)
public class SharedTenantSettingsPort implements TenantSettingsPort {
    @Override
    public boolean isOverageEnabled(UUID tenantId) {
        throw new UnsupportedOperationException("Shared tenant service not available");
    }

    @Override
    public void setOverageEnabled(UUID tenantId, boolean enabled) {
        throw new UnsupportedOperationException("Shared tenant service not available");
    }
}
