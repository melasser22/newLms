package com.ejada.billing.service;

import com.ejada.billing.dto.ServiceResult;
import com.ejada.billing.dto.TrackProductConsumptionRq;
import com.ejada.billing.dto.TrackProductConsumptionRs;

import java.util.UUID;

public interface ConsumptionService {

    /**
     * Implements POST /subscription/product-consumption/track
     * Header: rqUID, token
     * Body: TrackProductConsumptionRq
     * Returns: ServiceResult«TrackProductConsumptionRs»
     */
    ServiceResult<TrackProductConsumptionRs> trackProductConsumption(UUID rqUid, String token, TrackProductConsumptionRq rq);
}
