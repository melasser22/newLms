package com.ejada.catalog.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "AddonCreateReq", description = "Create a new addon")
public record AddonCreateReq(
        @NotBlank @Size(max = AddonCreateReq.MAX_CODE_LENGTH)
        @Schema(description = "Unique addon code", example = "EXTRA_SEATS")
        String addonCd,

        @NotBlank @Size(max = AddonCreateReq.MAX_NAME_LENGTH)
        @Schema(description = "English name", example = "Extra Seats")
        String addonEnNm,

        @NotBlank @Size(max = AddonCreateReq.MAX_NAME_LENGTH)
        @Schema(description = "Arabic name", example = "مقاعد إضافية")
        String addonArNm,

        @Schema(description = "Detailed description")
        String description,

        @Size(max = AddonCreateReq.MAX_CATEGORY_LENGTH)
        @Schema(description = "Optional category for UI grouping", example = "SEATS")
        String category,

        @Schema(description = "Active flag; defaults to true if null")
        Boolean isActive
) {
    private static final int MAX_CODE_LENGTH = 64;
    private static final int MAX_NAME_LENGTH = 128;
    private static final int MAX_CATEGORY_LENGTH = 64;
}
