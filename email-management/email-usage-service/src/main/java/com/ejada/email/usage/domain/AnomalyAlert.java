package com.ejada.email.usage.domain;

import java.time.LocalDate;

public record AnomalyAlert(
    String tenantId, LocalDate date, String reason, double metricValue, double baseline) {}
