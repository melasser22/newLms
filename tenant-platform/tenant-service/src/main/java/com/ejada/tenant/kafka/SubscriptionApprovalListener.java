package com.ejada.tenant.kafka;

import com.ejada.common.events.subscription.SubscriptionApprovalAction;
import com.ejada.common.events.subscription.SubscriptionApprovalMessage;
import com.ejada.common.events.subscription.SubscriptionApprovalProperties;
import com.ejada.tenant.dto.TenantCreateReq;
import com.ejada.tenant.exception.TenantConflictException;
import com.ejada.tenant.service.TenantService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Consumes subscription approval decisions and provisions tenants for approved subscriptions.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionApprovalListener {

    private final ObjectMapper objectMapper;
    private final TenantService tenantService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final SubscriptionApprovalProperties approvalProperties;
    @KafkaListener(
            topics = "${app.subscription-approval.topic}",
            groupId = "${app.subscription-approval.consumer-group}",
            containerFactory = "subscriptionApprovalListenerContainerFactory"
    )
    public void onMessage(@Payload final Map<String, Object> payload, Acknowledgment acknowledgment) {
        SubscriptionApprovalMessage message = objectMapper.convertValue(payload, SubscriptionApprovalMessage.class);

        if (message.action() != SubscriptionApprovalAction.APPROVED) {
            log.debug(
                    "Ignoring subscription approval message {} with action {}",
                    message.requestId(),
                    message.action());
            acknowledgment.acknowledge();
            return;
        }

        if (!StringUtils.hasText(message.tenantCode()) || !StringUtils.hasText(message.tenantName())) {
            throw new IllegalArgumentException(
                    "Missing tenant details in approval message %s".formatted(message.requestId()));
        }

        TenantCreateReq req = new TenantCreateReq(
                message.tenantCode(),
                message.tenantName(),
                message.contactEmail(),
                message.contactPhone(),
                null,
                Boolean.TRUE);

        try {
            tenantService.create(req);
            log.info(
                    "Tenant {} provisioned for subscription {} (approval request {})",
                    req.code(),
                    message.subscriptionId(),
                    message.requestId());
            acknowledgment.acknowledge();
        } catch (TenantConflictException conflict) {
            log.info(
                    "Tenant {} already exists while processing approval request {}",
                    req.code(),
                    message.requestId());
            acknowledgment.acknowledge();
        } catch (RuntimeException ex) {
            log.error(
                    "Failed to provision tenant {} for approval request {}. Routing to dead-letter topic.",
                    req.code(),
                    message.requestId(),
                    ex);
            publishToDeadLetterTopic(payload, message);
            acknowledgment.acknowledge();
        }
    }

    private void publishToDeadLetterTopic(
            Map<String, Object> payload, SubscriptionApprovalMessage message) {
        String deadLetterTopic = approvalProperties.getTopic() + ".dlt";
        try {
            kafkaTemplate
                    .send(deadLetterTopic, message.requestId(), payload)
                    .get(10, TimeUnit.SECONDS);
            log.warn(
                    "Routed subscription approval {} for tenant {} to dead-letter topic {}",
                    message.requestId(),
                    message.tenantCode(),
                    deadLetterTopic);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Interrupted while publishing subscription approval %s to dead-letter topic %s"
                            .formatted(message.requestId(), deadLetterTopic),
                    ie);
        } catch (ExecutionException | TimeoutException publishFailure) {
            throw new IllegalStateException(
                    "Failed to publish subscription approval %s to dead-letter topic %s"
                            .formatted(message.requestId(), deadLetterTopic),
                    publishFailure);
        }
    }
}
