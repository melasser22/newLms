package com.ejada.sec.config;

import com.ejada.common.events.subscription.SubscriptionApprovalProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SubscriptionApprovalProperties.class)
public class SubscriptionApprovalConfiguration {
}
