package com.ejada.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(name = "AddonRes", description = "Addon response")
public record AddonRes(
        Integer addonId,
        String addonCd,
        String addonEnNm,
        String addonArNm,
        String description,
        String category,
        Boolean isActive,
        Boolean isDeleted,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
