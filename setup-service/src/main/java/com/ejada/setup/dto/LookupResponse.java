package com.ejada.setup.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LookupResponse")
public record LookupResponse(
        Integer lookupItemId,
        String lookupItemCd,
        String lookupItemEnNm,
        String lookupItemArNm,
        String lookupGroupCode,
        String parentLookupId,
        Boolean isActive,
        String itemEnDescription,
        String itemArDescription
) {
}
