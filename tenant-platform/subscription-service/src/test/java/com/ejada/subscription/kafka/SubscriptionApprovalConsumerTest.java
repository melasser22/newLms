package com.ejada.subscription.kafka;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionApprovalConsumerTest {

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private SubscriptionFeatureRepository featureRepository;
    @Mock private SubscriptionAdditionalServiceRepository additionalServiceRepository;
    @Mock private TenantProvisioningPublisher provisioningPublisher;

    private SubscriptionApprovalConsumer consumer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        consumer = new SubscriptionApprovalConsumer(
                objectMapper,
                subscriptionRepository,
                featureRepository,
                additionalServiceRepository,
                provisioningPublisher);
    }

    @Test
    void approvedMessagePublishesProvisioningPayload() {
        UUID requestId = UUID.randomUUID();
        SubscriptionApprovalMessage message = new SubscriptionApprovalMessage(
                SubscriptionApprovalAction.APPROVED,
                requestId,
                123L,
                456L,
                "Customer EN",
                "Customer AR",
                "admin@example.com",
                "0500000000",
                "TEN-123",
                "Tenant Inc",
                "ops@example.com",
                "0500000001",
                "role",
                OffsetDateTime.now(),
                null);
        Map<String, Object> payload = objectMapper.convertValue(message, Map.class);

        Subscription subscription = new Subscription();
        subscription.setSubscriptionId(10L);
        subscription.setExtSubscriptionId(123L);
        when(subscriptionRepository.findByExtSubscriptionId(123L)).thenReturn(Optional.of(subscription));

        SubscriptionFeature feature = new SubscriptionFeature();
        feature.setFeatureCd("FEATURE_A");
        feature.setFeatureCount(5);
        when(featureRepository.findBySubscriptionSubscriptionId(10L)).thenReturn(List.of(feature));

        SubscriptionAdditionalService addon = new SubscriptionAdditionalService();
        addon.setProductAdditionalServiceId(99L);
        addon.setServiceCd("ADDON_A");
        addon.setServiceNameEn("Addon English");
        addon.setServiceNameAr("Addon Arabic");
        addon.setServicePrice(BigDecimal.TEN);
        addon.setTotalAmount(BigDecimal.ONE);
        addon.setCurrency("USD");
        addon.setIsCountable(Boolean.TRUE);
        addon.setRequestedCount(3L);
        addon.setPaymentTypeCd("ONE_TIME");
        when(additionalServiceRepository.findBySubscriptionSubscriptionId(10L)).thenReturn(List.of(addon));

        consumer.onApproval(payload);

        List<ProvisionedFeature> expectedFeatures = List.of(new ProvisionedFeature("FEATURE_A", 5));
        List<ProvisionedAddon> expectedAddons = List.of(new ProvisionedAddon(
                99L,
                "ADDON_A",
                "Addon English",
                "Addon Arabic",
                BigDecimal.TEN,
                BigDecimal.ONE,
                "USD",
                Boolean.TRUE,
                3L,
                "ONE_TIME"));

        verify(provisioningPublisher).publish(eq(message), eq(expectedFeatures), eq(expectedAddons));
    }

    @Test
    void nonApprovedMessagesAreIgnored() {
        SubscriptionApprovalMessage message = new SubscriptionApprovalMessage(
                SubscriptionApprovalAction.REQUEST,
                UUID.randomUUID(),
                123L,
                456L,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                OffsetDateTime.now(),
                null);
        Map<String, Object> payload = objectMapper.convertValue(message, Map.class);

        consumer.onApproval(payload);

        verify(provisioningPublisher, never())
                .publish(org.mockito.Mockito.any(), org.mockito.Mockito.anyList(), org.mockito.Mockito.anyList());
    }
}
