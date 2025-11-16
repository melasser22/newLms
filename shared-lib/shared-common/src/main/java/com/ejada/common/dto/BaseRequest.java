package com.ejada.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
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

    /** Timestamp recording when the resource was originally created. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(
            accessMode = Schema.AccessMode.READ_ONLY,
            description = "UTC timestamp (ISO-8601) capturing when the resource was created. Populated from the authenticated JWT.",
            example = "2024-05-10T08:15:30Z")
    private Instant createdAt;

    /** Identifier of the actor/system that created the resource. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(
            accessMode = Schema.AccessMode.READ_ONLY,
            description = "User or system identifier of the creator resolved from the authenticated JWT.",
            example = "user@example.com")
    private String createdBy;

    /** Timestamp recording the last update applied to the resource. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(
            accessMode = Schema.AccessMode.READ_ONLY,
            description = "UTC timestamp (ISO-8601) capturing when the resource was last updated. Populated from the authenticated JWT.",
            example = "2024-05-18T11:42:00Z")
    private Instant updatedAt;

    /** Identifier of the actor/system that last modified the resource. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(
            accessMode = Schema.AccessMode.READ_ONLY,
            description = "User or system identifier responsible for the latest modification, resolved from the authenticated JWT.",
            example = "admin@example.com")
    private String updatedBy;
}
