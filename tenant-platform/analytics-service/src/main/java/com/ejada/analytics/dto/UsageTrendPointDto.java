package com.ejada.analytics.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record UsageTrendPointDto(OffsetDateTime timestamp, BigDecimal usage, Long events) {}
