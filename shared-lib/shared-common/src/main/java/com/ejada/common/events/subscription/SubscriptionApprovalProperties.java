package com.ejada.common.events.subscription;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Service configuration for the subscription approval workflow.
 */
@Validated
@ConfigurationProperties(prefix = "app.subscription-approval")
public class SubscriptionApprovalProperties {

    /** Kafka topic used to exchange subscription approval messages. */
    @NotBlank
    private String topic = "tenant.subscription-approvals";

    /** Target application role that should review approval requests. */
    @NotBlank
    private String approvalRole = "ejada-officer";

    /** Consumer group used by services that process approval responses. */
    @NotBlank
    private String consumerGroup = "tenant-approval-listener";

    public String getTopic() {
        return topic;
    }

    public void setTopic(final String topic) {
        this.topic = topic;
    }

    public String getApprovalRole() {
        return approvalRole;
    }

    public void setApprovalRole(final String approvalRole) {
        this.approvalRole = approvalRole;
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public void setConsumerGroup(final String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }
}
