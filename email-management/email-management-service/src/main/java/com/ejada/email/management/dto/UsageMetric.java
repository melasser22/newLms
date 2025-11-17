package com.ejada.email.management.dto;

import java.time.LocalDate;

public record UsageMetric(LocalDate date, long delivered, long bounced, long complaints) {}
