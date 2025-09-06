package com.ejada.tenant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(name = "TenantUpdateReq")
public record TenantUpdateReq(
        @Size(max = 64)
        @Schema(description = "Unique tenant code", example = "EJADA")
        String code,

        @Size(max = 128)
        @Schema(description = "Tenant display name", example = "Ejada Systems")
        String name,

        @Email @Size(max = 255)
        @Schema(description = "Contact email", example = "ops@ejada.com")
        String contactEmail,

        @Size(max = 32)
        @Schema(description = "Contact phone (normalized E.164 preferred)", example = "+966500000000")
        String contactPhone,

        @Schema(description = "Active flag")
        Boolean active
) {}