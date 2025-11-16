package com.ejada.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
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

    /** Tenant identifier for multi-tenant requests (resolved from JWT). */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "Resolved from the authenticated JWT token")
    private UUID tenantId;

    /** Internal tenant identifier aligned with platform services (resolved from JWT). */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "Resolved from the authenticated JWT token")
    private UUID internalTenantId;
}
