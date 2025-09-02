package com.ejada.subscription.core;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SubscriptionService {

    private final SubscriptionQueryPort port;

    public SubscriptionService(SubscriptionQueryPort port) {
        this.port = port;
    }

    public SubscriptionQueryPort.ActiveSubscription active(UUID tenantId) {
        return port.loadActive(tenantId);
    }
}
