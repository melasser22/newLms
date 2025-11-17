package com.ejada.usage.dto;

import java.time.LocalDate;

public record UsageMetricDto(LocalDate date, long delivered, long bounced, long complaints) {}
