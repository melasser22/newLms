package com.lms.tenant.core;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TenantService {

    private final TenantSettingsPort settingsPort;

    public TenantService(TenantSettingsPort settingsPort) {
        this.settingsPort = settingsPort;
    }

    public void toggleOverage(UUID tenantId, boolean enabled) {
        settingsPort.setOverageEnabled(tenantId, enabled);
    }

    public boolean isOverageEnabled(UUID tenantId) {
        return settingsPort.isOverageEnabled(tenantId);
    }
}
