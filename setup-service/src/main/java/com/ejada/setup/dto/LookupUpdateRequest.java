package com.ejada.setup.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(name = "LookupUpdateRequest")
public record LookupUpdateRequest(
        @Size(max = 64)
        @Schema(description = "Lookup code", example = "COUNTRY")
        String lookupItemCd,

        @Size(max = 128)
        @Schema(description = "Lookup English name", example = "Country")
        String lookupItemEnNm,

        @Size(max = 128)
        @Schema(description = "Lookup Arabic name")
        String lookupItemArNm,

        @Size(max = 64)
        @Schema(description = "Lookup group code", example = "COUNTRY")
        String lookupGroupCode,

        @Size(max = 64)
        @Schema(description = "Parent lookup id when hierarchical")
        String parentLookupId,

        @Schema(description = "Whether lookup is active", example = "true")
        Boolean isActive,

        @Size(max = 512)
        @Schema(description = "English description")
        String itemEnDescription,

        @Size(max = 512)
        @Schema(description = "Arabic description")
        String itemArDescription
) {
}
