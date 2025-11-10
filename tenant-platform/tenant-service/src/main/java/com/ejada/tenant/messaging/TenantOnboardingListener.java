package com.ejada.tenant.messaging;

import com.ejada.common.events.tenant.TenantProvisioningEvent;
import com.ejada.tenant.properties.TenantKafkaTopicsProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TenantOnboardingListener {

    private final ObjectMapper objectMapper;
    private final TenantOnboardingService onboardingService;
    private final TenantKafkaTopicsProperties topics;

    @KafkaListener(topics = "#{@tenantKafkaTopicsProperties.tenantOnboarding()}")
    public void handleTenantProvisioning(@Payload final Map<String, Object> payload) {
        try {
            TenantProvisioningEvent event = objectMapper.convertValue(payload, TenantProvisioningEvent.class);
            onboardingService.createOrUpdateTenant(event);
        } catch (IllegalArgumentException ex) {
            log.error("Failed to map tenant provisioning payload: {}", payload, ex);
        } catch (Exception ex) {
            log.error("Unexpected error while handling tenant provisioning payload from topic {}", topics.tenantOnboarding(), ex);
            throw ex;
        }
    }
}
