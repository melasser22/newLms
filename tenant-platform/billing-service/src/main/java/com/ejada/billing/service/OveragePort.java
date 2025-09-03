package com.ejada.billing.service;


import java.util.UUID;

import com.ejada.tenant.core.dto.OverageResponse;
import com.ejada.tenant.core.dto.RecordOverageRequest;

/**
 * Port for persisting overage records.
 */
public interface OveragePort {

    OverageResponse recordOverage(UUID tenantId, UUID subscriptionId, RecordOverageRequest request);
}
