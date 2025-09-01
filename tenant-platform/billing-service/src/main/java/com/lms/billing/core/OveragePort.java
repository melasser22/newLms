package com.lms.billing.core;

import com.shared.billing.api.OverageResponse;
import com.shared.billing.api.RecordOverageRequest;

import java.util.UUID;

/**
 * Port for persisting overage records.
 */
public interface OveragePort {

    OverageResponse recordOverage(UUID tenantId, UUID subscriptionId, RecordOverageRequest request);
}
