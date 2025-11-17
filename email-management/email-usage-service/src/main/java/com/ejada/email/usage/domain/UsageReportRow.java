package com.ejada.usage.domain;

import java.time.LocalDate;

public record UsageReportRow(
    String tenantId,
    LocalDate usageDate,
    long sent,
    long delivered,
    long bounced,
    long opened,
    long spamComplaints,
    long quotaConsumed) {}
