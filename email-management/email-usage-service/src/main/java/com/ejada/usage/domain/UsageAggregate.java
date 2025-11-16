package com.ejada.usage.domain;

import java.time.LocalDate;

public record UsageAggregate(LocalDate date, long delivered, long bounced, long complaints) {}
