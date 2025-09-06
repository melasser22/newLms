package com.ejada.catalog.dto;

public record TierRes(
    Integer tierId,
    String tierCd,
    String tierEnNm,
    String tierArNm,
    String description,
    Integer rankOrder,
    Boolean isActive,
    Boolean isDeleted
) { }