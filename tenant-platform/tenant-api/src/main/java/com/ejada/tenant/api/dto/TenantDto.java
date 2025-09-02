package com.ejada.tenant.api.dto;

import java.util.UUID;

/**
 * Lightweight representation of a tenant in the system.
 *
 * @param id            tenant identifier
 * @param name          tenant display name
 * @param subscription  subscription currently associated with the tenant
 */
public record TenantDto(UUID id, String name, SubscriptionDto subscription) {
}

