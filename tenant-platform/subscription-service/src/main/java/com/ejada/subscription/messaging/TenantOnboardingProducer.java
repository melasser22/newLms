package com.ejada.subscription.messaging;

import com.ejada.common.events.tenant.TenantProvisioningEvent;
import com.ejada.common.events.tenant.TenantProvisioningEvent.TenantCustomerInfo;
import com.ejada.subscription.dto.CustomerInfoDto;
import com.ejada.subscription.dto.AdminUserInfoDto;
import com.ejada.subscription.model.Subscription;
import com.ejada.subscription.properties.SubscriptionKafkaTopicsProperties;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "KafkaTemplate is a Spring-managed bean and safe to retain")
public class TenantOnboardingProducer {

    private final KafkaTemplate<String, TenantProvisioningEvent> kafkaTemplate;
    private final SubscriptionKafkaTopicsProperties topics;

    public void publishTenantCreateRequested(final Subscription subscription,
            final CustomerInfoDto customerInfo,
            final AdminUserInfoDto adminUserInfo) {

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

        Class<?> adminInfoClass = resolveAdminInfoClass();
        Object payloadAdmin = adminInfoClass == null
                ? null
                : buildAdminInfoPayload(adminInfoClass, adminUserInfo);

        String extSubscriptionId = subscription.getExtSubscriptionId() == null
                ? null
                : subscription.getExtSubscriptionId().toString();
        String extCustomerId = subscription.getExtCustomerId() == null
                ? null
                : subscription.getExtCustomerId().toString();

        TenantProvisioningEvent event = buildEvent(
                subscription,
                extSubscriptionId,
                extCustomerId,
                payloadCustomer,
                payloadAdmin,
                adminInfoClass);

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

    private Class<?> resolveAdminInfoClass() {
        try {
            return Class.forName("com.ejada.common.events.tenant.TenantProvisioningEvent$TenantAdminInfo");
        } catch (ClassNotFoundException ex) {
            log.debug("Tenant admin info payload type not present; proceeding without admin details");
            return null;
        }
    }

    private Object buildAdminInfoPayload(final Class<?> adminInfoClass, final AdminUserInfoDto adminUserInfo) {
        if (adminUserInfo == null) {
            return null;
        }
        try {
            Constructor<?> constructor = adminInfoClass.getDeclaredConstructor(
                    String.class, String.class, String.class, String.class);
            return constructor.newInstance(
                    adminUserInfo.adminUserName(),
                    adminUserInfo.email(),
                    adminUserInfo.mobileNo(),
                    adminUserInfo.preferredLang());
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Failed to construct tenant admin info payload", ex);
        }
    }

    private TenantProvisioningEvent buildEvent(
            final Subscription subscription,
            final String extSubscriptionId,
            final String extCustomerId,
            final TenantCustomerInfo payloadCustomer,
            final Object payloadAdmin,
            final Class<?> adminInfoClass) {

        if (adminInfoClass != null) {
            try {
                Constructor<?> constructor = TenantProvisioningEvent.class.getDeclaredConstructor(
                        Long.class,
                        String.class,
                        String.class,
                        TenantCustomerInfo.class,
                        adminInfoClass);
                return (TenantProvisioningEvent) constructor.newInstance(
                        subscription.getSubscriptionId(),
                        extSubscriptionId,
                        extCustomerId,
                        payloadCustomer,
                        payloadAdmin);
            } catch (NoSuchMethodException ex) {
                log.debug(
                        "TenantProvisioningEvent does not expose admin payload constructor; falling back to customer-only payload",
                        ex);
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException("Failed to construct tenant provisioning event with admin payload", ex);
            }
        }

        try {
            Constructor<TenantProvisioningEvent> constructor = TenantProvisioningEvent.class.getDeclaredConstructor(
                    Long.class,
                    String.class,
                    String.class,
                    TenantCustomerInfo.class);
            return constructor.newInstance(
                    subscription.getSubscriptionId(),
                    extSubscriptionId,
                    extCustomerId,
                    payloadCustomer);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Failed to construct tenant provisioning event", ex);
        }
    }
}
