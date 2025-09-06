package com.ejada.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(name = "AddonUpdateReq", description = "Patch/update an existing addon")
public record AddonUpdateReq(
        @Size(max = CODE_MAX)
        @Schema(description = "Unique addon code", example = "EXTRA_SEATS")
        String addonCd,

        @Size(max = EN_NAME_MAX)
        @Schema(description = "English name", example = "Extra Seats")
        String addonEnNm,

        @Size(max = AR_NAME_MAX)
        @Schema(description = "Arabic name", example = "مقاعد إضافية")
        String addonArNm,

        @Schema(description = "Detailed description")
        String description,

        @Size(max = CATEGORY_MAX)
        @Schema(description = "Optional category for UI grouping", example = "SEATS")
        String category,

        @Schema(description = "Active flag")
        Boolean isActive
) {
    public static final int CODE_MAX = 64;
    public static final int EN_NAME_MAX = 128;
    public static final int AR_NAME_MAX = 128;
    public static final int CATEGORY_MAX = 64;
}

