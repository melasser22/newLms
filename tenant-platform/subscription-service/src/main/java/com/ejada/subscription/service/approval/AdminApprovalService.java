package com.ejada.subscription.service.approval;

import com.ejada.common.constants.ErrorCodes;
import com.ejada.common.dto.ServiceResult;
import com.ejada.common.events.subscription.SubscriptionApprovalAction;
import com.ejada.common.events.subscription.SubscriptionApprovalMessage;
import com.ejada.common.marketplace.subscription.dto.CustomerInfoDto;
import com.ejada.subscription.dto.admin.AdminApproveSubscriptionRequest;
import com.ejada.subscription.dto.admin.AdminApproveSubscriptionResponse;
import com.ejada.subscription.kafka.SubscriptionApprovalPublisher;
import com.ejada.subscription.model.Subscription;
import com.ejada.subscription.model.SubscriptionActivityLog;
import com.ejada.subscription.model.SubscriptionApprovalRequest;
import com.ejada.subscription.model.SubscriptionApprovalStatus;
import com.ejada.subscription.repository.SubscriptionActivityLogRepository;
import com.ejada.subscription.repository.SubscriptionApprovalRequestRepository;
import com.ejada.subscription.repository.SubscriptionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Handles administrator driven approvals of subscription requests.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminApprovalService {

    private final SubscriptionApprovalRequestRepository approvalRequestRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionActivityLogRepository activityLogRepository;
    private final SubscriptionApprovalPublisher approvalPublisher;
    private final ApprovalActorProvider actorProvider;
    private final ObjectMapper objectMapper;

    @Transactional
    public ServiceResult<AdminApproveSubscriptionResponse> approve(
            final Long approvalRequestId, final AdminApproveSubscriptionRequest command) {

        SubscriptionApprovalRequest approvalRequest =
                approvalRequestRepository.findById(approvalRequestId).orElse(null);
        if (approvalRequest == null) {
            return ServiceResult.error(
                    null,
                    ErrorCodes.NOT_FOUND,
                    "Approval request not found",
                    List.of("approvalRequestId=" + approvalRequestId));
        }
        if (!"PENDING".equalsIgnoreCase(approvalRequest.getStatus())) {
            return ServiceResult.error(
                    null,
                    ErrorCodes.BUSINESS_RULE_VIOLATION,
                    "Approval request cannot be processed",
                    List.of("status=" + approvalRequest.getStatus()));
        }

        Subscription subscription = approvalRequest.getSubscription();
        if (subscription == null) {
            return ServiceResult.error(
                    null,
                    ErrorCodes.INTERNAL_ERROR,
                    "Approval request is missing subscription reference",
                    List.of("approvalRequestId=" + approvalRequestId));
        }

        ApprovalActorProvider.ApprovalActor actor = actorProvider.currentActor();
        OffsetDateTime now = OffsetDateTime.now();

        transitionApprovalRequest(approvalRequest, command, actor, now);
        transitionSubscription(subscription, actor, now);

        approvalRequestRepository.save(approvalRequest);
        subscriptionRepository.save(subscription);
        recordActivity(approvalRequest, subscription, command, actor);

        CustomerInfoDto customerInfo = parseCustomerInfo(approvalRequest.getTenantInfoJson());
        SubscriptionApprovalMessage message = approvalPublisher.publishApprovalDecision(
                SubscriptionApprovalAction.APPROVED,
                UUID.randomUUID(),
                subscription,
                customerInfo,
                null,
                null,
                command.approvalNotes());

        AdminApproveSubscriptionResponse response = new AdminApproveSubscriptionResponse(
                approvalRequest.getApprovalRequestId(),
                subscription.getSubscriptionId(),
                subscription.getExtSubscriptionId(),
                subscription.getExtCustomerId(),
                subscription.getApprovalStatus(),
                subscription.getSubscriptionSttsCd(),
                subscription.getApprovedAt(),
                subscription.getApprovedBy(),
                approvalRequest.getApproverEmail(),
                message.requestId(),
                message.tenantCode(),
                message.tenantName(),
                message.customerNameEn(),
                message.customerNameAr(),
                command.shouldNotifyCustomer());

        String requestId = message.requestId() != null ? message.requestId().toString() : null;
        return ServiceResult.ok(requestId, response, "Subscription approved successfully");
    }

    private void transitionApprovalRequest(
            final SubscriptionApprovalRequest approvalRequest,
            final AdminApproveSubscriptionRequest command,
            final ApprovalActorProvider.ApprovalActor actor,
            final OffsetDateTime now) {

        approvalRequest.setStatus("APPROVED");
        approvalRequest.setApprovedAt(now);
        approvalRequest.setApprovedBy(actor.username());
        approvalRequest.setApproverEmail(actor.email());
        approvalRequest.setApprovalNotes(command.approvalNotes());
        approvalRequest.setProcessedAt(now);
        approvalRequest.setRejectionNotes(null);
        approvalRequest.setRejectionReason(null);
        approvalRequest.setRejectedAt(null);
        approvalRequest.setRejectedBy(null);
    }

    private void transitionSubscription(
            final Subscription subscription,
            final ApprovalActorProvider.ApprovalActor actor,
            final OffsetDateTime now) {
        subscription.setApprovalStatus(SubscriptionApprovalStatus.APPROVED.name());
        subscription.setApprovalRequired(Boolean.FALSE);
        subscription.setApprovedAt(now);
        subscription.setApprovedBy(actor.username());
        subscription.setRejectedAt(null);
        subscription.setRejectedBy(null);
        subscription.setSubscriptionSttsCd("ACTIVE");
        subscription.setUpdatedAt(now);
        subscription.setUpdatedBy(actor.username());
    }

    private void recordActivity(
            final SubscriptionApprovalRequest approvalRequest,
            final Subscription subscription,
            final AdminApproveSubscriptionRequest command,
            final ApprovalActorProvider.ApprovalActor actor) {
        SubscriptionActivityLog activity = new SubscriptionActivityLog();
        activity.setSubscription(subscription);
        activity.setActivityType("APPROVED");
        activity.setDescription(
                StringUtils.hasText(command.approvalNotes())
                        ? command.approvalNotes()
                        : "Subscription approved by " + actor.displayName());
        activity.setPerformedBy(actor.username());
        activity.setMetadata(buildMetadata(approvalRequest, subscription, command, actor));
        activityLogRepository.save(activity);
    }

    private String buildMetadata(
            final SubscriptionApprovalRequest approvalRequest,
            final Subscription subscription,
            final AdminApproveSubscriptionRequest command,
            final ApprovalActorProvider.ApprovalActor actor) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("approvalRequestId", approvalRequest.getApprovalRequestId());
        metadata.put("subscriptionId", subscription.getSubscriptionId());
        metadata.put("approvalNotes", command.approvalNotes());
        metadata.put("additionalChecks", command.additionalChecks());
        metadata.put("notifyCustomer", command.shouldNotifyCustomer());
        metadata.put("approvedBy", actor.username());
        metadata.put("approverEmail", actor.email());
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize approval metadata", ex);
            return null;
        }
    }

    private CustomerInfoDto parseCustomerInfo(final String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, CustomerInfoDto.class);
        } catch (Exception ex) {
            log.warn("Unable to deserialize customer info for approval request", ex);
            return null;
        }
    }
}
