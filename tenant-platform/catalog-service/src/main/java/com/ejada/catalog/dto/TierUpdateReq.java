package com.ejada.catalog.dto;

public record TierUpdateReq(
    String tierEnNm,
    String tierArNm,
    String description,
    Integer rankOrder,
    Boolean isActive,
    Boolean isDeleted
) { }