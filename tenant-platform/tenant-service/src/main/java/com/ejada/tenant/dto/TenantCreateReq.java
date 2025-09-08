package com.ejada.tenant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "TenantCreateReq")
public record TenantCreateReq(
        @NotBlank @Size(max = 64)
        @Schema(description = "Unique tenant code", example = "EJADA")
        String code,

        @NotBlank @Size(max = 128)
        @Schema(description = "Tenant display name", example = "Ejada Systems")
        String name,

        @Email @Size(max = 255)
        @Schema(description = "Contact email", example = "ops@ejada.com")
        String contactEmail,

        @Size(max = 32)
        @Schema(description = "Contact phone (normalized E.164 preferred)", example = "+966500000000")
        String contactPhone,

        @Size(max = 255)
        @Schema(description = "Tenant logo URL", example = "https://example.com/logo.png")
        String logoUrl,

        @Schema(description = "Active flag; defaults to true if null")
        Boolean active
) { }
