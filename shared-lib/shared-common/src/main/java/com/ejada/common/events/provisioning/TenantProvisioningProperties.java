package com.ejada.common.events.provisioning;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for tenant provisioning Kafka topic & consumer group.
 */
@ConfigurationProperties(prefix = "app.tenant-provisioning")
public class TenantProvisioningProperties {

    /** Kafka topic carrying tenant provisioning messages. */
    private String topic = "tenant.provisioning";

    /** Consumer group used by services reacting to tenant provisioning. */
    private String consumerGroup = "tenant-provisioning-listener";

    public String getTopic() {
        return topic;
    }

    public void setTopic(final String topic) {
        this.topic = topic;
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public void setConsumerGroup(final String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }
}
