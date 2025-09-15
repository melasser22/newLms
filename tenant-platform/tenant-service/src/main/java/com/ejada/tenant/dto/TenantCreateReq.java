package com.ejada.tenant.dto;

import static com.ejada.tenant.model.Tenant.CODE_LENGTH;
import static com.ejada.tenant.model.Tenant.EMAIL_LENGTH;
import static com.ejada.tenant.model.Tenant.LOGO_URL_LENGTH;
import static com.ejada.tenant.model.Tenant.NAME_LENGTH;
import static com.ejada.tenant.model.Tenant.PHONE_LENGTH;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "TenantCreateReq")
public record TenantCreateReq(
        @NotBlank @Size(max = CODE_LENGTH)
        @Schema(description = "Unique tenant code", example = "EJADA")
        String code,

        @NotBlank @Size(max = NAME_LENGTH)
        @Schema(description = "Tenant display name", example = "Ejada Systems")
        String name,

        @Email @Size(max = EMAIL_LENGTH)
        @Schema(description = "Contact email", example = "ops@ejada.com")
        String contactEmail,

        @Size(max = PHONE_LENGTH)
        @Schema(description = "Contact phone (normalized E.164 preferred)", example = "+966500000000")
        String contactPhone,

        @Size(max = LOGO_URL_LENGTH)
        @Schema(description = "Tenant logo URL", example = "https://example.com/logo.png")
        String logoUrl,

        @Schema(description = "Active flag; defaults to true if null")
        Boolean active
) { }
