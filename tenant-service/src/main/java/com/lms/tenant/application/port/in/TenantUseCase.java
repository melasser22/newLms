package com.lms.tenant.application.port.in;

import com.lms.tenant.domain.Tenant;

import java.util.Optional;
import java.util.UUID;

/**
 * Input port defining the use cases for tenant management.
 * This is the entry point to the application's core logic.
 */
public interface TenantUseCase {

    /**
     * Creates a new tenant.
     *
     * @param tenant The tenant domain object containing the creation data (e.g., from a DTO).
     * @return The newly created tenant.
     */
    Tenant createTenant(Tenant tenant);

    /**
     * Finds a tenant by its unique ID.
     *
     * @param id The UUID of the tenant.
     * @return An Optional containing the tenant if found.
     */
    Optional<Tenant> findTenantById(UUID id);

    /**
     * Finds a tenant by its unique slug.
     *
     * @param slug The tenant slug.
     * @return An Optional containing the tenant if found.
     */
    Optional<Tenant> findTenantBySlug(String slug);

    /**
     * Toggles the overage policy for a tenant.
     *
     * @param tenantId The ID of the tenant.
     * @param isEnabled The desired state for the overage policy.
     * @return The updated tenant.
     */
    Tenant setOveragePolicy(UUID tenantId, boolean isEnabled);
}
