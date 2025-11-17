package com.ejada.email.usage.domain;

import java.time.LocalDate;

public record UsageTrendPoint(
    LocalDate date,
    long sent,
    long delivered,
    long bounced,
    long opened,
    long spamComplaints) {}
