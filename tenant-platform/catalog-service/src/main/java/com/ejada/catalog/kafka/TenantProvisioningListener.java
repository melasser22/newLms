package com.ejada.catalog.kafka;

import com.ejada.catalog.service.TenantProvisioningService;
import com.ejada.common.events.provisioning.TenantProvisioningMessage;
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
public class TenantProvisioningListener {

    private final ObjectMapper objectMapper;
    private final TenantProvisioningService provisioningService;

    @KafkaListener(
            topics = "#{@tenantProvisioningProperties.topic}",
            groupId = "#{@tenantProvisioningProperties.consumerGroup}"
    )
    public void onMessage(@Payload final Map<String, Object> payload) {
        TenantProvisioningMessage message = objectMapper.convertValue(payload, TenantProvisioningMessage.class);
        log.info("Applying provisioning update for tenant {}", message.tenantCode());
        provisioningService.applyProvisioning(message);
    }
}
