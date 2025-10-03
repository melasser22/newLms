package com.ejada.subscription.service.approval;

import com.ejada.common.marketplace.subscription.dto.ReceiveSubscriptionNotificationRq;
import com.ejada.subscription.model.Subscription;
import com.ejada.subscription.model.SubscriptionApprovalRequest;
import com.ejada.subscription.model.SubscriptionApprovalStatus;
import com.ejada.subscription.model.SubscriptionActivityLog;
import com.ejada.subscription.repository.SubscriptionActivityLogRepository;
import com.ejada.subscription.repository.SubscriptionApprovalRequestRepository;
import com.ejada.subscription.repository.SubscriptionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalWorkflowService {

    private static final String SYSTEM_USER = "SYSTEM";
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionApprovalRequestRepository approvalRequestRepository;
    private final SubscriptionActivityLogRepository activityLogRepository;
    private final RiskAssessmentService riskAssessmentService;
    private final AutoApprovalService autoApprovalService;
    private final ObjectMapper objectMapper;

    @Transactional
    public SubmissionResult submitForApproval(
            final Subscription subscription,
            final ReceiveSubscriptionNotificationRq request,
            final boolean isNew) {

        String currentStatus = subscription.getApprovalStatus();
        if (!isNew) {
            if (SubscriptionApprovalStatus.isApproved(currentStatus)) {
                return SubmissionResult.alreadyApproved(subscription);
            }
            if (SubscriptionApprovalStatus.isPending(currentStatus)) {
                return SubmissionResult.alreadyPending(subscription);
            }
        }

        OffsetDateTime now = OffsetDateTime.now();
        subscription.setApprovalRequired(Boolean.TRUE);
        subscription.setApprovalStatus(SubscriptionApprovalStatus.PENDING_APPROVAL.name());
        subscription.setSubscriptionSttsCd("PENDING_APPROVAL");
        subscription.setSubmittedAt(now);
        subscription.setSubmittedBy(SYSTEM_USER);
        subscription.setApprovedAt(null);
        subscription.setApprovedBy(null);
        subscription.setRejectedAt(null);
        subscription.setRejectedBy(null);
        subscriptionRepository.save(subscription);

        SubscriptionApprovalRequest approvalRequest = new SubscriptionApprovalRequest();
        approvalRequest.setSubscription(subscription);
        approvalRequest.setStatus("PENDING");
        approvalRequest.setRequestedAt(now);
        approvalRequest.setRequestedBy(SYSTEM_USER);
        approvalRequest.setDueDate(now.plusHours(24));
        approvalRequest.setPriority("NORMAL");
        approvalRequest.setTenantInfoJson(writeJsonSafe(request.customerInfo()));
        approvalRequest.setSubscriptionInfoJson(writeJsonSafe(request.subscriptionInfo()));

        RiskAssessmentService.RiskAssessmentResult risk =
                riskAssessmentService.evaluate(request, subscription);
        approvalRequest.setRiskScore(risk.riskScore());
        approvalRequest.setRiskLevel(risk.riskLevel());
        if (risk.riskScore() >= 85) {
            approvalRequest.setPriority("URGENT");
            approvalRequest.setRequiresAdditionalReview(Boolean.TRUE);
        }

        approvalRequestRepository.save(approvalRequest);
        recordActivity(subscription, "SUBMITTED_FOR_APPROVAL", "Subscription received from external system");

        AutoApprovalService.AutoApprovalDecision decision =
                autoApprovalService.evaluate(subscription, approvalRequest);
        if (decision.shouldApprove()) {
            return autoApprove(subscription, approvalRequest, decision.ruleTriggered());
        }

        return SubmissionResult.pending(subscription, approvalRequest);
    }

    private SubmissionResult autoApprove(
            final Subscription subscription,
            final SubscriptionApprovalRequest approvalRequest,
            final String ruleTriggered) {
        OffsetDateTime now = OffsetDateTime.now();
        approvalRequest.setStatus("AUTO_APPROVED");
        approvalRequest.setApprovedAt(now);
        approvalRequest.setApprovedBy("AUTO_APPROVAL_SYSTEM");
        approvalRequest.setApprovalNotes("Auto-approved by rule: " + ruleTriggered);
        approvalRequest.setProcessedAt(now);
        approvalRequestRepository.save(approvalRequest);

        subscription.setApprovalStatus(SubscriptionApprovalStatus.AUTO_APPROVED.name());
        subscription.setApprovalRequired(Boolean.FALSE);
        subscription.setApprovedAt(now);
        subscription.setApprovedBy("AUTO_APPROVAL_SYSTEM");
        subscription.setSubscriptionSttsCd("ACTIVE");
        subscriptionRepository.save(subscription);

        recordActivity(subscription, "AUTO_APPROVED", "Automatically approved by rule " + ruleTriggered);
        return SubmissionResult.autoApproved(subscription, approvalRequest, ruleTriggered);
    }

    private void recordActivity(
            final Subscription subscription, final String activityType, final String description) {
        SubscriptionActivityLog logEntry = new SubscriptionActivityLog();
        logEntry.setSubscription(subscription);
        logEntry.setActivityType(activityType);
        logEntry.setDescription(description);
        logEntry.setPerformedBy(SYSTEM_USER);
        activityLogRepository.save(logEntry);
    }

    private String writeJsonSafe(final Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            log.warn("Failed to serialize payload for approval request", ex);
            return null;
        }
    }

    public record SubmissionResult(
            SubmissionState state,
            Subscription subscription,
            SubscriptionApprovalRequest approvalRequest,
            String autoApprovalRule) {

        public static SubmissionResult pending(
                final Subscription subscription, final SubscriptionApprovalRequest request) {
            return new SubmissionResult(SubmissionState.PENDING, subscription, request, null);
        }

        public static SubmissionResult autoApproved(
                final Subscription subscription,
                final SubscriptionApprovalRequest request,
                final String rule) {
            return new SubmissionResult(SubmissionState.AUTO_APPROVED, subscription, request, rule);
        }

        public static SubmissionResult alreadyApproved(final Subscription subscription) {
            return new SubmissionResult(SubmissionState.ALREADY_APPROVED, subscription, null, null);
        }

        public static SubmissionResult alreadyPending(final Subscription subscription) {
            return new SubmissionResult(SubmissionState.ALREADY_PENDING, subscription, null, null);
        }
    }

    public enum SubmissionState {
        PENDING,
        AUTO_APPROVED,
        ALREADY_APPROVED,
        ALREADY_PENDING
    }
}
