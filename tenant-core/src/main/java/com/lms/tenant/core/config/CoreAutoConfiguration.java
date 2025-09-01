package com.lms.tenant.core.config;

import com.lms.tenant.core.port.FeaturePolicyPort;
import com.lms.tenant.core.port.OveragePort;
import com.lms.tenant.core.port.SubscriptionPort;
import com.lms.tenant.core.port.TenantSettingsPort;
import com.lms.tenant.core.service.OverageService;
import com.lms.tenant.core.service.PolicyService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CoreAutoConfiguration {

    @Bean
    public OverageService overageService(OveragePort overagePort) {
        return new OverageService(overagePort);
    }

    @Bean
    public PolicyService policyService(SubscriptionPort subscriptionPort,
                                       FeaturePolicyPort featurePolicyPort,
                                       TenantSettingsPort tenantSettingsPort,
                                       OverageService overageService) {
        return new PolicyService(subscriptionPort, featurePolicyPort, tenantSettingsPort, overageService);
    }
}
