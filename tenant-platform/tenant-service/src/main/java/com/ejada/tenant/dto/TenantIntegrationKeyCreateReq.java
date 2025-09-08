package com.ejada.tenant.dto;

import static com.ejada.tenant.model.TenantIntegrationKey.KEY_ID_LENGTH;
import static com.ejada.tenant.model.TenantIntegrationKey.LABEL_LENGTH;
import static com.ejada.tenant.model.TenantIntegrationKey.PLAIN_SECRET_LENGTH;
import static com.ejada.tenant.model.TenantIntegrationKey.SCOPE_LENGTH;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;

@Schema(name = "TenantIntegrationKeyCreateReq")
public record TenantIntegrationKeyCreateReq(
        @NotNull
        @Schema(description = "Owning tenant ID", example = "1")
        Integer tenantId,

        @NotBlank @Size(max = KEY_ID_LENGTH)
        @Schema(description = "Public key identifier", example = "SYS_DEFAULT")
        String keyId,

        @Size(max = PLAIN_SECRET_LENGTH)
        @Schema(description = "Optional plain secret to be hashed server-side; generated if blank", example = "S3cure-Plain-Secret")
        String plainSecret,

        @Size(max = LABEL_LENGTH)
        @Schema(description = "Friendly label", example = "Backoffice API key")
        String label,

        @Schema(description = "List of scopes/permissions")
        List<@NotBlank @Size(max = SCOPE_LENGTH) String> scopes,

        @NotNull
        @Schema(description = "Status", example = "ACTIVE")
        TikStatus status,

        @Schema(description = "Validity start; defaults to now if null")
        OffsetDateTime validFrom,

        @NotNull
        @Future
        @Schema(description = "Hard expiry timestamp")
        OffsetDateTime expiresAt,

        @Positive
        @Schema(description = "Optional per-day quota")
        Integer dailyQuota,

        @Schema(description = "Arbitrary JSON metadata (stringified if not using json type mapping)")
        String meta
) {
    public TenantIntegrationKeyCreateReq {
        scopes = scopes == null ? List.of() : List.copyOf(scopes);
    }

    @Override
    public List<String> scopes() {
        return List.copyOf(scopes);
    }
}