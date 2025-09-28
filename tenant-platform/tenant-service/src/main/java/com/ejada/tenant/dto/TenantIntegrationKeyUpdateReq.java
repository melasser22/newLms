package com.ejada.tenant.dto;

import static com.ejada.tenant.model.TenantIntegrationKey.LABEL_LENGTH;
import static com.ejada.tenant.model.TenantIntegrationKey.SCOPE_LENGTH;
import static com.ejada.tenant.model.TenantIntegrationKey.AUDIT_NAME_LENGTH;
import static com.ejada.tenant.model.TenantIntegrationKey.PLAIN_SECRET_LENGTH;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;

@Schema(name = "TenantIntegrationKeyUpdateReq")
public record TenantIntegrationKeyUpdateReq(
        @Size(max = LABEL_LENGTH)
        @Schema(description = "Friendly label", example = "Backoffice API key")
        String label,

        @Schema(description = "List of scopes/permissions")
        List<@NotBlank @Size(max = SCOPE_LENGTH) String> scopes,

        @Schema(description = "Status", example = "SUSPENDED")
        TikStatus status,

        @Schema(description = "Validity start (cannot be after expiresAt)")
        OffsetDateTime validFrom,

        @Future
        @Schema(description = "Hard expiry timestamp")
        OffsetDateTime expiresAt,

        @Positive
        @Schema(description = "Optional per-day quota")
        Integer dailyQuota,

        @Schema(description = "Arbitrary JSON metadata")
        String meta,

        @Size(max = PLAIN_SECRET_LENGTH)
        @Schema(description = "Provide to rotate the secret; hashed and never persisted in plaintext")
        String newPlainSecret,

        @Size(max = AUDIT_NAME_LENGTH)
        @Schema(description = "Actor rotating the secret", example = "tenant-admin")
        String rotatedBy
) {
    public TenantIntegrationKeyUpdateReq {
        scopes = scopes == null ? List.of() : List.copyOf(scopes);
    }

    @Override
    public List<String> scopes() {
        return List.copyOf(scopes);
    }
}