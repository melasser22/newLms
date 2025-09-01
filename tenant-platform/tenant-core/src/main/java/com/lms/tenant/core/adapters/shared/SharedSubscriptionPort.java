package com.lms.tenant.core.adapters.shared;

import com.lms.tenant.core.SubscriptionPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnClass(name = "com.shared.subscription.api.SubscriptionQueryService")
@ConditionalOnMissingBean(SubscriptionPort.class)
public class SharedSubscriptionPort implements SubscriptionPort {
    @Override
    public ActiveSubscription activeSubscription(UUID tenantId) {
        throw new UnsupportedOperationException("Shared subscription service not available");
    }
}
