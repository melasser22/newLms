package com.ejada.catalog.dto;

public record TierCreateReq(
    String tierCd,
    String tierEnNm,
    String tierArNm,
    String description,
    Integer rankOrder,
    Boolean isActive
) { }

