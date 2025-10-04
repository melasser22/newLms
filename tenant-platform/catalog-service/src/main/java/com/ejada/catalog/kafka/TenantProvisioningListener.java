package com.ejada.catalog.kafka;

import com.ejada.catalog.service.TenantProvisioningService;
import com.ejada.common.events.provisioning.TenantProvisioningMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TenantProvisioningListener {

    private final Function<Map<String, Object>, TenantProvisioningMessage> messageConverter;
    private final TenantProvisioningService provisioningService;

    public TenantProvisioningListener(final ObjectMapper objectMapper,
                                      final TenantProvisioningService provisioningService) {
        ObjectMapper mapper = objectMapper != null ? objectMapper.copy() : new ObjectMapper();
        this.messageConverter = payload -> mapper.convertValue(payload, TenantProvisioningMessage.class);
        this.provisioningService = provisioningService;
    }

    @KafkaListener(
            topics = "#{@tenantProvisioningProperties.topic}",
            groupId = "#{@tenantProvisioningProperties.consumerGroup}"
    )
    public void onMessage(@Payload final Map<String, Object> payload) {
        TenantProvisioningMessage message = messageConverter.apply(payload);
        if (message == null) {
            log.warn("Ignoring provisioning payload that could not be converted into a provisioning message");
            return;
        }

        log.info("Applying provisioning update for tenant {}", message.tenantCode());
        provisioningService.applyProvisioning(message);
    }
}
