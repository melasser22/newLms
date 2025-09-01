package com.lms.tenant.subscription.adapter;

import com.lms.tenant.subscription.service.SubscriptionService;
import com.shared.subscription.api.SubscriptionDto;
import com.shared.subscription.api.SubscriptionQueryService;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionQueryServiceAdapter implements SubscriptionQueryService {

    private final SubscriptionService subscriptionService;

    public SubscriptionQueryServiceAdapter(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @Override
    public Optional<SubscriptionDto> findActiveSubscription(UUID tenantId) {
        return subscriptionService.findActiveSubscription(tenantId);
    }
}
