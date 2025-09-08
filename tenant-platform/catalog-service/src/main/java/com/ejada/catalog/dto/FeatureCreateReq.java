package com.ejada.catalog.dto;

public record FeatureCreateReq(
        String featureKey,
        String featureEnNm,
        String featureArNm,
        String description,
        String category,
        Boolean isMetered,
        Boolean isActive
) { }