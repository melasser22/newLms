package com.ejada.subscription.kafka;

import com.ejada.common.events.provisioning.ProvisionedAddon;
import com.ejada.common.events.provisioning.ProvisionedFeature;
import com.ejada.common.events.subscription.SubscriptionApprovalAction;
import com.ejada.common.events.subscription.SubscriptionApprovalMessage;
import com.ejada.subscription.model.Subscription;
import com.ejada.subscription.model.SubscriptionAdditionalService;
import com.ejada.subscription.model.SubscriptionFeature;
import com.ejada.subscription.repository.SubscriptionAdditionalServiceRepository;
import com.ejada.subscription.repository.SubscriptionFeatureRepository;
import com.ejada.subscription.repository.SubscriptionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reacts to administrator subscription approvals by publishing provisioning payloads for
 * downstream services (catalog, billing, etc.).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionApprovalConsumer {

    private final ObjectMapper objectMapper;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionFeatureRepository featureRepository;
    private final SubscriptionAdditionalServiceRepository additionalServiceRepository;
    private final TenantProvisioningPublisher provisioningPublisher;

    @KafkaListener(
            topics = "${app.subscription-approval.topic}",
            groupId = "${app.subscription-approval.consumer-group}"
    )
    @Transactional(readOnly = true)
    public void onApproval(@Payload final Map<String, Object> payload) {
        SubscriptionApprovalMessage message = objectMapper.convertValue(payload, SubscriptionApprovalMessage.class);

        if (message.action() != SubscriptionApprovalAction.APPROVED) {
            log.debug(
                    "Skipping approval event {} because action is {}",
                    message.requestId(),
                    message.action());
            return;
        }

        if (message.subscriptionId() == null) {
            log.warn("Approval message {} lacks subscriptionId, unable to provision", message.requestId());
            return;
        }

        Subscription subscription = subscriptionRepository
                .findByExtSubscriptionId(message.subscriptionId())
                .orElseThrow(() -> new IllegalStateException(
                        "Unknown subscription " + message.subscriptionId() + " in approval event"));

        List<ProvisionedFeature> features = featureRepository
                .findBySubscriptionSubscriptionId(subscription.getSubscriptionId())
                .stream()
                .map(this::mapFeature)
                .toList();

        List<ProvisionedAddon> addons = additionalServiceRepository
                .findBySubscriptionSubscriptionId(subscription.getSubscriptionId())
                .stream()
                .map(this::mapAddon)
                .toList();

        provisioningPublisher.publish(message, features, addons);
    }

    private ProvisionedFeature mapFeature(final SubscriptionFeature entity) {
        return new ProvisionedFeature(entity.getFeatureCd(), entity.getFeatureCount());
    }

    private ProvisionedAddon mapAddon(final SubscriptionAdditionalService entity) {
        return new ProvisionedAddon(
                entity.getProductAdditionalServiceId(),
                entity.getServiceCd(),
                entity.getServiceNameEn(),
                entity.getServiceNameAr(),
                entity.getServicePrice(),
                entity.getTotalAmount(),
                entity.getCurrency(),
                entity.getIsCountable(),
                entity.getRequestedCount(),
                entity.getPaymentTypeCd());
    }
}
