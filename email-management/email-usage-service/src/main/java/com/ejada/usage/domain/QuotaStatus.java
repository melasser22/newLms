package com.ejada.usage.domain;

public record QuotaStatus(
    String tenantId,
    long monthlyQuota,
    long monthlyUsed,
    long dailyBurstLimit,
    long todaysUsage,
    int alertThresholdPercent,
    boolean quotaExceeded,
    boolean burstExceeded) {}
