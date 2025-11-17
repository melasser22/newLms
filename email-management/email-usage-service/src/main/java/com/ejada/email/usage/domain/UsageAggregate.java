package com.ejada.email.usage.domain;

import java.time.LocalDate;

public record UsageAggregate(LocalDate date, long delivered, long bounced, long complaints) {}
