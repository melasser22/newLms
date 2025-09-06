package com.ejada.catalog.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(name = "AddonCreateReq", description = "Create a new addon")
public record AddonCreateReq(
        @NotBlank @Size(max = 64)
        @Schema(description = "Unique addon code", example = "EXTRA_SEATS")
        String addonCd,

        @NotBlank @Size(max = 128)
        @Schema(description = "English name", example = "Extra Seats")
        String addonEnNm,

        @NotBlank @Size(max = 128)
        @Schema(description = "Arabic name", example = "مقاعد إضافية")
        String addonArNm,

        @Schema(description = "Detailed description")
        String description,

        @Size(max = 64)
        @Schema(description = "Optional category for UI grouping", example = "SEATS")
        String category,

        @Schema(description = "Active flag; defaults to true if null")
        Boolean isActive
) {}