package com.ejada.subscription.kafka;

import com.ejada.common.events.subscription.SubscriptionApprovalAction;
import com.ejada.common.events.subscription.SubscriptionApprovalMessage;
import com.ejada.common.events.subscription.SubscriptionApprovalProperties;
import com.ejada.common.marketplace.subscription.dto.ReceiveSubscriptionNotificationRq;
import com.ejada.subscription.model.Subscription;
import com.ejada.subscription.tenant.TenantLink;
import com.ejada.subscription.tenant.TenantLinkFactory;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Publishes Kafka messages requesting administrator approval for freshly created subscriptions.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionApprovalPublisher {

    private static final int TENANT_CODE_MAX = 64;
    private static final int TENANT_NAME_MAX = 128;

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final SubscriptionApprovalProperties properties;
    private final TenantLinkFactory tenantLinkFactory;

    /**
     * Sends an approval request message for the supplied subscription.
     *
     * @param rqUid   unique request identifier coming from the inbound marketplace notification
     * @param request original marketplace payload
     * @param subscription persisted subscription entity
     */
    public SubscriptionApprovalMessage publishApprovalRequest(
            final UUID rqUid,
            final ReceiveSubscriptionNotificationRq request,
            final Subscription subscription) {
        return publishApprovalDecision(
                SubscriptionApprovalAction.REQUEST, rqUid, request, subscription, null);
    }

    public SubscriptionApprovalMessage publishApprovalDecision(
            final SubscriptionApprovalAction action,
            final UUID rqUid,
            final ReceiveSubscriptionNotificationRq request,
            final Subscription subscription,
            final TenantLink tenantLink) {

        UUID requestId = Optional.ofNullable(rqUid).orElseGet(UUID::randomUUID);
        TenantLink resolvedLink =
                tenantLink != null ? tenantLink : tenantLinkFactory.resolve(request, subscription);
        SubscriptionApprovalMessage message =
                buildMessage(action, requestId, request, subscription, resolvedLink);

        try {
            SendResult<String, Object> result =
                    kafkaTemplate
                            .send(properties.getTopic(), requestId.toString(), message)
                            .join();

            log.info(
                    "Published subscription approval request for subscription {} to topic {} (partition={}, offset={})",
                    subscription.getExtSubscriptionId(),
                    properties.getTopic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            return message;
        } catch (CompletionException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            log.error(
                    "Failed to publish approval request for subscription {}",
                    subscription.getExtSubscriptionId(),
                    cause);
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException("Unable to publish subscription approval request", cause);
        }
    }

    private SubscriptionApprovalMessage buildMessage(
            final SubscriptionApprovalAction action,
            final UUID requestId,
            final ReceiveSubscriptionNotificationRq request,
            final Subscription subscription,
            final TenantLink tenantLink) {

        String tenantCode = tenantLink != null ? tenantLink.tenantCode() : null;
        String tenantName = tenantLink != null ? tenantLink.tenantName() : null;

        return new SubscriptionApprovalMessage(
                action,
                requestId,
                subscription.getExtSubscriptionId(),
                subscription.getExtCustomerId(),
                safeTrim(request.customerInfo().customerNameEn(), TENANT_NAME_MAX),
                safeTrim(request.customerInfo().customerNameAr(), TENANT_NAME_MAX),
                request.adminUserInfo().email(),
                request.adminUserInfo().mobileNo(),
                tenantCode,
                tenantName,
                safeTrim(request.customerInfo().email(), 255),
                safeTrim(request.customerInfo().mobileNo(), 32),
                properties.getApprovalRole(),
                OffsetDateTime.now(),
                null);
    }

    private String safeTrim(final String value, final int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

}
