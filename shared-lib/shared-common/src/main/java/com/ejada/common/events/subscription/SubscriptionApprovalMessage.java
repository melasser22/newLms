package com.ejada.common.events.subscription;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Shared Kafka payload used to coordinate subscription approval decisions between services.
 */
public record SubscriptionApprovalMessage(
        SubscriptionApprovalAction action,
        UUID requestId,
        Long subscriptionId,
        Long customerId,
        String customerNameEn,
        String customerNameAr,
        String adminEmail,
        String adminMobile,
        String tenantCode,
        String tenantName,
        String contactEmail,
        String contactPhone,
        String approvalRole,
        OffsetDateTime timestamp,
        String notes) {
}
