package com.ejada.subscription.kafka;

import com.ejada.common.events.provisioning.ProvisionedAddon;
import com.ejada.common.events.provisioning.ProvisionedFeature;
import com.ejada.common.events.provisioning.TenantProvisioningMessage;
import com.ejada.common.events.provisioning.TenantProvisioningProperties;
import com.ejada.common.events.subscription.SubscriptionApprovalMessage;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

/** Publishes tenant provisioning payloads once an approval decision is received. */
@Component
@RequiredArgsConstructor
@Slf4j
public class TenantProvisioningPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final TenantProvisioningProperties properties;

    public void publish(
            final SubscriptionApprovalMessage approval,
            final List<ProvisionedFeature> features,
            final List<ProvisionedAddon> addons) {

        UUID key = approval.requestId() != null ? approval.requestId() : UUID.randomUUID();
        TenantProvisioningMessage message = new TenantProvisioningMessage(
                approval.requestId(),
                approval.subscriptionId(),
                approval.tenantCode(),
                approval.tenantName(),
                features,
                addons,
                OffsetDateTime.now());

        try {
            SendResult<String, Object> result =
                    kafkaTemplate.send(properties.getTopic(), key.toString(), message).join();
            log.info(
                    "Published tenant provisioning event for tenant {} on topic {} (partition={}, offset={})",
                    approval.tenantCode(),
                    properties.getTopic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
        } catch (Exception ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            log.error("Failed to publish tenant provisioning message for tenant {}", approval.tenantCode(), cause);
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException("Unable to publish tenant provisioning message", cause);
        }
    }
}
