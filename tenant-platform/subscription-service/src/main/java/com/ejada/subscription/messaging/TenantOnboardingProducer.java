package com.ejada.subscription.messaging;

import com.ejada.common.events.tenant.TenantProvisioningEvent;
import com.ejada.common.events.tenant.TenantProvisioningEvent.TenantCustomerInfo;
import com.ejada.subscription.dto.CustomerInfoDto;
import com.ejada.subscription.model.Subscription;
import com.ejada.subscription.properties.SubscriptionKafkaTopicsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TenantOnboardingProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final SubscriptionKafkaTopicsProperties topics;

    public void publishTenantCreateRequested(final Subscription subscription,
            final CustomerInfoDto customerInfo) {

        if (subscription == null) {
            log.warn("Skipping tenant onboarding publish: subscription is null");
            return;
        }
        if (customerInfo == null) {
            log.warn("Skipping tenant onboarding publish for subscription {}: missing customer info",
                    subscription.getSubscriptionId());
            return;
        }

        TenantCustomerInfo payloadCustomer = new TenantCustomerInfo(
                customerInfo.customerNameEn(),
                customerInfo.customerNameAr(),
                customerInfo.customerType(),
                customerInfo.crNumber(),
                customerInfo.countryCd(),
                customerInfo.cityCd(),
                customerInfo.addressEn(),
                customerInfo.addressAr(),
                customerInfo.email(),
                customerInfo.mobileNo());

        TenantProvisioningEvent event = new TenantProvisioningEvent(
                subscription.getSubscriptionId(),
                subscription.getExtSubscriptionId(),
                subscription.getExtCustomerId(),
                payloadCustomer);

        String key = subscription.getExtCustomerId();
        kafkaTemplate.send(topics.tenantOnboarding(), key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish tenant onboarding event for subscription {}", subscription.getSubscriptionId(), ex);
                    } else if (result != null) {
                        log.info("Published tenant onboarding event to {} partition {} offset {}", result.getRecordMetadata().topic(), result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                    }
                });
    }
}
