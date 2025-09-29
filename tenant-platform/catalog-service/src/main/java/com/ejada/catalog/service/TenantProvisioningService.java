package com.ejada.catalog.service;

import com.ejada.common.events.provisioning.TenantProvisioningMessage;

public interface TenantProvisioningService {
    void applyProvisioning(TenantProvisioningMessage message);
}
