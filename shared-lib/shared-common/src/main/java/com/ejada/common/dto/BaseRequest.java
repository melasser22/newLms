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
    @Schema(
            description = "UTC timestamp (ISO-8601) capturing when the resource was created. Required for create requests.",
            example = "2024-05-10T08:15:30Z")
    private Instant createdAt;

    /** Identifier of the actor/system that created the resource. */
    @Schema(description = "User or system identifier of the creator. Required for create requests.", example = "user@example.com")
    private String createdBy;

    /** Timestamp recording the last update applied to the resource. */
    @Schema(
            description = "UTC timestamp (ISO-8601) capturing when the resource was last updated. Required for update/delete requests.",
            example = "2024-05-18T11:42:00Z")
    private Instant updatedAt;

    /** Identifier of the actor/system that last modified the resource. */
    @Schema(description = "User or system identifier responsible for the latest modification.", example = "admin@example.com")
    private String updatedBy;
}
