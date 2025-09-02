package com.ejada.tenant.events;

import java.util.UUID;

/**
 * Event published when a tenant's overage setting is toggled.
 *
 * @param tenantId       the tenant identifier
 * @param overageEnabled whether overage was enabled after the toggle
 */
public record TenantOverageToggledEvent(UUID tenantId, boolean overageEnabled) {}
