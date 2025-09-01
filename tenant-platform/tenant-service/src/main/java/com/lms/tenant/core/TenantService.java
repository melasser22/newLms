package com.lms.tenant.core;

import com.lms.tenant.events.publisher.OutboxService;
import com.lms.tenant.events.tenants.OverageToggleChanged;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class TenantService {

    private final TenantSettingsPort settingsPort;
    private final OutboxService outbox;

    public TenantService(TenantSettingsPort settingsPort,
                         OutboxService outbox) {
        this.settingsPort = settingsPort;
        this.outbox = outbox;
    }

    public void toggleOverage(UUID tenantId, boolean enabled) {
        settingsPort.setOverageEnabled(tenantId, enabled);
        outbox.append(new OverageToggleChanged(tenantId, enabled), Map.of());
    }

    public boolean isOverageEnabled(UUID tenantId) {
        return settingsPort.isOverageEnabled(tenantId);
    }
}
