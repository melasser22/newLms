package com.ejada.subscription.dto.admin;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response payload returned after a successful manual approval.
 */
public record AdminApproveSubscriptionResponse(
        Long approvalRequestId,
        Long subscriptionId,
        Long extSubscriptionId,
        Long extCustomerId,
        String approvalStatus,
        String subscriptionStatus,
        OffsetDateTime approvedAt,
        String approvedBy,
        String approverEmail,
        UUID approvalEventId,
        String tenantCode,
        String tenantName,
        String customerNameEn,
        String customerNameAr,
        boolean notifyCustomer) {}
