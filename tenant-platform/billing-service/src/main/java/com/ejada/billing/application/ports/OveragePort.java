package com.ejada.billing.application.ports;


import java.util.UUID;

import com.ejada.billing.domain.dtos.OverageResponse;
import com.ejada.billing.domain.dtos.RecordOverageRequest;

/**
 * Port for persisting overage records.
 */
public interface OveragePort {

    OverageResponse recordOverage(UUID tenantId, UUID subscriptionId, RecordOverageRequest request);
}
