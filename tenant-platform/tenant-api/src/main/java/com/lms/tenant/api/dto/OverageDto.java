package com.lms.tenant.api.dto;

/**
 * Represents usage that exceeded an allotted limit for a feature.
 *
 * @param featureCode identifier for the feature
 * @param allowed     quota allowed for the feature
 * @param used        amount actually used
 * @param overage     amount over the allowed quota
 */
public record OverageDto(String featureCode, long allowed, long used, long overage) {
}

