package com.ejada.common.events.provisioning;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Payload broadcast after a tenant has been provisioned so downstream services can
 * synchronize entitlements (features, addons, etc.).
 */
public record TenantProvisioningMessage(
        UUID requestId,
        Long subscriptionId,
        String tenantCode,
        String tenantName,
        List<ProvisionedFeature> features,
        List<ProvisionedAddon> addons,
        OffsetDateTime timestamp) {
}
