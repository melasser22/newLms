package com.ejada.analytics.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PeakUsageWindowDto(
    OffsetDateTime windowStart, Long eventCount, BigDecimal totalUsage) {}
