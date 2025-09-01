package com.lms.entitlement.controller.dto;

import java.time.Instant;

public record ManualOverageRequest(String featureKey, long quantity, long unitPriceMinor,
                                   String currency, Instant periodStart, Instant periodEnd, String idemKey) {
}
