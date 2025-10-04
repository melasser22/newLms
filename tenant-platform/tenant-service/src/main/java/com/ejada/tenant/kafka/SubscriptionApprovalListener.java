package com.ejada.tenant.kafka;

import com.ejada.common.events.subscription.SubscriptionApprovalAction;
import com.ejada.common.events.subscription.SubscriptionApprovalMessage;
import com.ejada.common.tenant.TenantIdentifiers;
import com.ejada.tenant.dto.TenantCreateReq;
import com.ejada.tenant.exception.TenantConflictException;
import com.ejada.tenant.service.TenantService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Consumes subscription approval decisions and provisions tenants for approved subscriptions.
 */
@Component
@Slf4j
public class SubscriptionApprovalListener {

    private final ObjectMapper objectMapper;
    private final TenantService tenantService;

    public SubscriptionApprovalListener(final ObjectMapper objectMapper, final TenantService tenantService) {
        this.objectMapper = objectMapper.copy();
        this.tenantService = tenantService;
    }
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
                Boolean.TRUE,
                TenantIdentifiers.deriveTenantId(message.tenantCode()));

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
                    "Failed to provision tenant {} for approval request {}. Will be retried or dead-lettered by the container.",
                    req.code(),
                    message.requestId(),
                    ex);
            throw ex;
        }
    }
}
