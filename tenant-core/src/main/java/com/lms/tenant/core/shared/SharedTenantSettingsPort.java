package com.lms.tenant.core.shared;

import com.lms.tenant.core.port.TenantSettingsPort;

import java.lang.reflect.Method;
import java.util.UUID;

/** Adapter using shared-library TenantService. */
class SharedTenantSettingsPort implements TenantSettingsPort {

    private final Object service;

    SharedTenantSettingsPort(Object service) {
        this.service = service;
    }

    @Override
    public boolean isOverageEnabled(UUID tenantId) {
        try {
            Method m = service.getClass().getMethod("getSettings", UUID.class);
            Object settings = m.invoke(service, tenantId);
            Method enabled = settings.getClass().getMethod("overageEnabled");
            return (Boolean) enabled.invoke(settings);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load tenant settings", e);
        }
    }

    @Override
    public void setOverageEnabled(UUID tenantId, boolean enabled) {
        try {
            Method m = service.getClass().getMethod("updateOverageEnabled", UUID.class, boolean.class);
            m.invoke(service, tenantId, enabled);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to update tenant settings", e);
        }
    }
}
