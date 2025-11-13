package com.ejada.common.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Standard request wrapper containing common request fields.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BaseRequest {

    /** Tenant identifier for multi-tenant requests. */
    @NotNull
    private UUID tenantId;

    /** Internal tenant identifier aligned with platform services. */
    @NotNull
    private UUID internalTenantId;
}
