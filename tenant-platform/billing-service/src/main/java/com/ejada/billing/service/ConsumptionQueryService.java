package com.ejada.billing.service;

import com.ejada.billing.dto.ProductSubscriptionStts;

public interface ConsumptionQueryService {
    ProductSubscriptionStts getSnapshot(Long extSubscriptionId, Long customerIdNullable);
}
