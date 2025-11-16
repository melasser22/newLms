package com.ejada.usage.domain;

import java.time.LocalDate;

public record UsageSummary(
    String tenantId,
    LocalDate from,
    LocalDate to,
    long sent,
    long delivered,
    long bounced,
    long opened,
    long clicked,
    long spamComplaints,
    double deliveryRate,
    double bounceRate,
    double openRate) {}
