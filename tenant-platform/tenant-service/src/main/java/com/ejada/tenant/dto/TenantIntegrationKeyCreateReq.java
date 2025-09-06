package com.ejada.tenant.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.List;

@Schema(name = "TenantIntegrationKeyCreateReq")
public record TenantIntegrationKeyCreateReq(
        @NotNull
        @Schema(description = "Owning tenant ID", example = "1")
        Integer tenantId,

        @NotBlank @Size(max = 64)
        @Schema(description = "Public key identifier", example = "SYS_DEFAULT")
        String keyId,

        @Size(min = 12, max = 256)
        @Schema(description = "Plain secret to be hashed server-side (optional if you auto-generate)", example = "S3cure-Plain-Secret")
        String plainSecret,

        @Size(max = 128)
        @Schema(description = "Friendly label", example = "Backoffice API key")
        String label,

        @Schema(description = "List of scopes/permissions")
        List<@NotBlank @Size(max = 64) String> scopes,

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