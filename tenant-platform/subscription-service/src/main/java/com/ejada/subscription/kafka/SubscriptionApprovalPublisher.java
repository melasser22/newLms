package com.ejada.subscription.kafka;

import com.ejada.common.events.subscription.SubscriptionApprovalAction;
import com.ejada.common.events.subscription.SubscriptionApprovalMessage;
import com.ejada.common.events.subscription.SubscriptionApprovalProperties;
import com.ejada.common.marketplace.subscription.dto.AdminUserInfoDto;
import com.ejada.common.marketplace.subscription.dto.CustomerInfoDto;
import com.ejada.common.marketplace.subscription.dto.ReceiveSubscriptionNotificationRq;
import com.ejada.subscription.model.Subscription;
import com.ejada.subscription.tenant.TenantLink;
import com.ejada.subscription.tenant.TenantLinkFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
    private static final int EMAIL_MAX = 255;
    private static final int PHONE_MAX = 32;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Kafka template is managed bean")
    private final KafkaTemplate<String, Object> kafkaTemplate;
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Properties bean is immutable for our use")
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
        TenantLink tenantLink = tenantLinkFactory.resolve(request, subscription);
        CustomerInfoDto customerInfo = request != null ? request.customerInfo() : null;
        AdminUserInfoDto adminUserInfo = request != null ? request.adminUserInfo() : null;
        return publishApprovalDecision(
                SubscriptionApprovalAction.REQUEST,
                rqUid,
                subscription,
                customerInfo,
                adminUserInfo,
                tenantLink,
                null);
    }

    public SubscriptionApprovalMessage publishApprovalDecision(
            final SubscriptionApprovalAction action,
            final UUID requestId,
            final Subscription subscription,
            final CustomerInfoDto customerInfo,
            final AdminUserInfoDto adminUserInfo,
            final TenantLink tenantLink,
            final String notes) {

        UUID resolvedRequestId = Optional.ofNullable(requestId).orElseGet(UUID::randomUUID);
        TenantLink resolvedLink =
                tenantLink != null ? tenantLink : tenantLinkFactory.resolve(customerInfo, subscription);
        SubscriptionApprovalMessage message =
                buildMessage(action, resolvedRequestId, subscription, customerInfo, adminUserInfo, resolvedLink, notes);

        try {
            SendResult<String, Object> result =
                    kafkaTemplate
                            .send(properties.getTopic(), resolvedRequestId.toString(), message)
                            .join();

            log.info(
                    "Published subscription approval {} for subscription {} to topic {} (partition={}, offset={})",
                    action,
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
            final Subscription subscription,
            final CustomerInfoDto customerInfo,
            final AdminUserInfoDto adminUserInfo,
            final TenantLink tenantLink,
            final String notes) {

        String tenantCode = tenantLink != null ? tenantLink.tenantCode() : null;
        String tenantName = tenantLink != null ? tenantLink.tenantName() : null;

        String customerNameEn = customerInfo != null ? customerInfo.customerNameEn() : null;
        String customerNameAr = customerInfo != null ? customerInfo.customerNameAr() : null;
        String contactEmail = customerInfo != null ? customerInfo.email() : null;
        String contactPhone = customerInfo != null ? customerInfo.mobileNo() : null;
        String adminEmail = adminUserInfo != null ? adminUserInfo.email() : null;
        String adminMobile = adminUserInfo != null ? adminUserInfo.mobileNo() : null;

        String resolvedAdminEmail = safeTrim(firstNonBlank(adminEmail, contactEmail), EMAIL_MAX);
        String resolvedAdminMobile = safeTrim(firstNonBlank(adminMobile, contactPhone), PHONE_MAX);

        return new SubscriptionApprovalMessage(
                action,
                requestId,
                subscription.getExtSubscriptionId(),
                subscription.getExtCustomerId(),
                safeTrim(customerNameEn, TENANT_NAME_MAX),
                safeTrim(customerNameAr, TENANT_NAME_MAX),
                resolvedAdminEmail,
                resolvedAdminMobile,
                safeTrim(tenantCode, TENANT_CODE_MAX),
                tenantName,
                safeTrim(contactEmail, EMAIL_MAX),
                safeTrim(contactPhone, PHONE_MAX),
                properties.getApprovalRole(),
                OffsetDateTime.now(),
                notes);
    }

    private String safeTrim(final String value, final int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private String firstNonBlank(final String first, final String second) {
        if (StringUtils.hasText(first)) {
            return first.trim();
        }
        if (StringUtils.hasText(second)) {
            return second.trim();
        }
        return null;
    }

}
