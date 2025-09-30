package com.ejada.subscription.kafka;

import com.ejada.common.events.subscription.SubscriptionApprovalAction;
import com.ejada.common.events.subscription.SubscriptionApprovalMessage;
import com.ejada.common.events.subscription.SubscriptionApprovalProperties;
import com.ejada.common.marketplace.dto.ReceiveSubscriptionNotificationRq;
import com.ejada.subscription.model.Subscription;
import java.time.OffsetDateTime;
import java.util.Locale;
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

    /**
     * Sends an approval request message for the supplied subscription.
     *
     * @param rqUid   unique request identifier coming from the inbound marketplace notification
     * @param request original marketplace payload
     * @param subscription persisted subscription entity
     */
    public void publishApprovalRequest(
            final UUID rqUid,
            final ReceiveSubscriptionNotificationRq request,
            final Subscription subscription) {

        UUID requestId = Optional.ofNullable(rqUid).orElseGet(UUID::randomUUID);
        SubscriptionApprovalMessage message = buildMessage(requestId, request, subscription);

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
            final UUID requestId,
            final ReceiveSubscriptionNotificationRq request,
            final Subscription subscription) {

        String tenantCode = defaultTenantCode(subscription);
        String tenantName = defaultTenantName(request, tenantCode);

        return new SubscriptionApprovalMessage(
                SubscriptionApprovalAction.REQUEST,
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

    private String defaultTenantCode(final Subscription subscription) {
        Long customerId = subscription.getExtCustomerId();
        String base = customerId != null
                ? "CUST-" + customerId
                : "SUB-" + Optional.ofNullable(subscription.getExtSubscriptionId()).orElse(0L);
        String normalized = base.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9-]", "-");
        return safeTrim(normalized, TENANT_CODE_MAX);
    }

    private String defaultTenantName(
            final ReceiveSubscriptionNotificationRq request,
            final String tenantCode) {
        String primary = firstNonBlank(
                request.customerInfo().customerNameEn(),
                request.customerInfo().customerNameAr(),
                tenantCode);
        return safeTrim(primary, TENANT_NAME_MAX);
    }

    private String safeTrim(final String value, final int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private String firstNonBlank(final String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
