package com.ejada.sec.messaging;

import com.ejada.common.events.tenant.TenantProvisioningEvent;
import com.ejada.sec.config.SecKafkaTopicsProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TenantOnboardingListener {

    private final ObjectMapper objectMapper;
    private final TenantAdminProvisioningService provisioningService;
    private final SecKafkaTopicsProperties topics;

    public TenantOnboardingListener(
            final ObjectMapper objectMapper,
            final TenantAdminProvisioningService provisioningService,
            final SecKafkaTopicsProperties topics) {
        this.objectMapper = objectMapper.copy();
        this.provisioningService = provisioningService;
        this.topics = topics;
    }

    @KafkaListener(topics = "${sec.kafka.topics.tenant-onboarding}")
    public void handleTenantProvisioning(@Payload final String payload) {
        try {
            TenantProvisioningEvent event = objectMapper.readValue(payload, TenantProvisioningEvent.class);
            provisioningService.provisionTenantAdmin(event);
        } catch (JsonProcessingException ex) {
            log.error("Failed to map tenant provisioning payload: {}", payload, ex);
        } catch (Exception ex) {
            log.error("Unexpected error while handling tenant provisioning payload from topic {}", topics.tenantOnboarding(), ex);
            throw ex;
        }
    }
}
