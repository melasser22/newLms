package com.ejada.sec.config;

import com.ejada.common.events.subscription.SubscriptionApprovalProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SubscriptionApprovalConfiguration {

    @Bean(name = "subscriptionApprovalProperties")
    @ConfigurationProperties(prefix = "app.subscription-approval")
    public SubscriptionApprovalProperties subscriptionApprovalProperties() {
        return new SubscriptionApprovalProperties();
    }
}
