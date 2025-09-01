package com.lms.entitlement.controller.dto;

public record OverageOverrideRequest(Boolean allowOverage, Long overageUnitPriceMinor, String overageCurrency) {
}
