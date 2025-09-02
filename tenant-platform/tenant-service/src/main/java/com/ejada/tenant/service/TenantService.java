package com.ejada.tenant.service;

import com.ejada.common.context.TenantContext;
import com.ejada.tenant.persistence.entity.Tenant;
import com.ejada.tenant.persistence.entity.TenantIntegrationKey;
import com.ejada.tenant.persistence.entity.enums.KeyStatus;
import com.ejada.tenant.persistence.entity.enums.TenantStatus;
import com.ejada.tenant.persistence.repository.TenantIntegrationKeyRepository;
import com.ejada.tenant.persistence.repository.TenantRepository;
import com.ejada.tenant.events.TenantOverageToggledEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TenantService {
    private final TenantRepository tenantRepository;
    private final TenantIntegrationKeyRepository keyRepository;
    private final TenantSettingsPort settingsPort;
    private final ApplicationEventPublisher eventPublisher;

    public TenantService(TenantRepository tenantRepository,
                         TenantIntegrationKeyRepository keyRepository,
                         TenantSettingsPort settingsPort,
                         ApplicationEventPublisher eventPublisher) {
        this.tenantRepository = tenantRepository;
        this.keyRepository = keyRepository;
        this.settingsPort = settingsPort;
        this.eventPublisher = eventPublisher;
    }

    public Tenant createTenant(String slug, String name) {
        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setSlug(slug);
        tenant.setName(name);
        tenant.setStatus(TenantStatus.ACTIVE);
        tenantRepository.save(tenant);

        // create default integration key respecting RLS
        TenantContext.set(tenant.getId());
        try {
            TenantIntegrationKey key = new TenantIntegrationKey();
            key.setId(UUID.randomUUID());
            key.setTenant(tenant);
            key.setName("default");
            key.setKey(UUID.randomUUID().toString());
            key.setStatus(KeyStatus.ACTIVE);
            keyRepository.save(key);
        } finally {
            TenantContext.clear();
        }
        return tenant;
    }

    public Tenant getTenant(UUID tenantId) {
        return tenantRepository.findById(tenantId).orElse(null);
    }

    public long getTotalTenantsByStatus(TenantStatus status) {
        return tenantRepository.countByStatus(status);
    }

    public void setOverage(UUID tenantId, boolean enabled) {
        settingsPort.setOverageEnabled(tenantId, enabled);
        eventPublisher.publishEvent(new TenantOverageToggledEvent(tenantId, enabled));
    }
}
