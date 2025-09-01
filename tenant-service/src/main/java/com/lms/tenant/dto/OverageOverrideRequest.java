package com.lms.tenant.dto;

public record OverageOverrideRequest(Boolean allowOverage, Long overageUnitPriceMinor, String overageCurrency) {
}
