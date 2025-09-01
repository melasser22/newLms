package com.lms.tenant.dto;

import java.time.Instant;

public record ManualOverageRequest(String featureKey, long quantity, long unitPriceMinor,
                                   String currency, Instant periodStart, Instant periodEnd, String idemKey) {
}
