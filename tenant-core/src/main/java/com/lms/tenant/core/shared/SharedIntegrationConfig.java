package com.lms.tenant.core.shared;

import com.lms.tenant.core.port.FeaturePolicyPort;
import com.lms.tenant.core.port.OveragePort;
import com.lms.tenant.core.port.SubscriptionPort;
import com.lms.tenant.core.port.TenantSettingsPort;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

@Configuration
public class SharedIntegrationConfig {

    @Bean
    @ConditionalOnClass(name = "com.shared.billing.api.OverageService")
    OveragePort sharedOveragePort(ApplicationContext context) throws ClassNotFoundException {
        Object svc = context.getBean(Class.forName("com.shared.billing.api.OverageService"));
        return new SharedOveragePort(svc);
    }

    @Bean
    @ConditionalOnClass(name = "com.shared.catalog.api.FeaturePolicyService")
    FeaturePolicyPort sharedFeaturePolicyPort(ApplicationContext context) throws ClassNotFoundException {
        Object svc = context.getBean(Class.forName("com.shared.catalog.api.FeaturePolicyService"));
        return new SharedFeaturePolicyPort(svc);
    }

    @Bean
    @ConditionalOnClass(name = "com.shared.subscription.api.SubscriptionQueryService")
    SubscriptionPort sharedSubscriptionPort(ApplicationContext context) throws ClassNotFoundException {
        Object svc = context.getBean(Class.forName("com.shared.subscription.api.SubscriptionQueryService"));
        return new SharedSubscriptionPort(svc);
    }

    @Bean
    @ConditionalOnClass(name = "com.shared.tenant.api.TenantService")
    TenantSettingsPort sharedTenantSettingsPort(ApplicationContext context) throws ClassNotFoundException {
        Object svc = context.getBean(Class.forName("com.shared.tenant.api.TenantService"));
        return new SharedTenantSettingsPort(svc);
    }
}
