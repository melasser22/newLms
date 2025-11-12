package com.ejada.subscription.messaging;

import com.ejada.common.events.tenant.TenantProvisioningEvent;
import com.ejada.common.events.tenant.TenantProvisioningEvent.TenantCustomerInfo;
import com.ejada.subscription.dto.CustomerInfoDto;
import com.ejada.subscription.model.Subscription;
import com.ejada.subscription.properties.SubscriptionKafkaTopicsProperties;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "KafkaTemplate is a Spring-managed bean and safe to retain")
public class TenantOnboardingProducer {

    private final KafkaTemplate<String, TenantProvisioningEvent> kafkaTemplate;
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

        String extSubscriptionId = subscription.getExtSubscriptionId() == null
                ? null
                : subscription.getExtSubscriptionId().toString();
        String extCustomerId = subscription.getExtCustomerId() == null
                ? null
                : subscription.getExtCustomerId().toString();

        TenantProvisioningEvent event = new TenantProvisioningEvent(
                subscription.getSubscriptionId(),
                extSubscriptionId,
                extCustomerId,
                payloadCustomer);

        String topic = topics.tenantOnboarding();
        String key = extCustomerId;

        CompletableFuture<SendResult<String, TenantProvisioningEvent>> sendResultFuture =
                key == null ? kafkaTemplate.send(topic, event) : kafkaTemplate.send(topic, key, event);

        sendResultFuture.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish tenant onboarding event for subscription {}", subscription.getSubscriptionId(), ex);
            } else if (result != null) {
                log.info("Published tenant onboarding event to {} partition {} offset {}", result.getRecordMetadata().topic(), result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            }
        });
    }
}
