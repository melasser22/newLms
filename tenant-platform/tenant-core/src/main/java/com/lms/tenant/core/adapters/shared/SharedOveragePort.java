package com.lms.tenant.core.adapters.shared;

import com.lms.tenant.core.OveragePort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@ConditionalOnClass(name = "com.shared.billing.api.OverageService")
@ConditionalOnMissingBean(OveragePort.class)
public class SharedOveragePort implements OveragePort {
    @Override
    public UUID recordOverage(UUID tenantId, UUID subscriptionId, String featureKey, long quantity, long unitPriceMinor, String currency, Instant periodStart, Instant periodEnd, String idempotencyKey) {
        throw new UnsupportedOperationException("Shared billing service not available");
    }
}
