package com.ejada.billing.controller;

import com.ejada.billing.dto.TrackProductConsumptionRq;
import com.ejada.billing.dto.TrackProductConsumptionRs;
import com.ejada.billing.service.ConsumptionService;
import com.ejada.common.dto.ServiceResult;
import com.ejada.common.exception.ServiceResultException;
import com.ejada.common.web.ServiceResultResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * POST /subscription/product-consumption/track
 * Headers:
 *  - rqUID: UUID (required)
 *  - token: String (required; swagger shows a UUID pattern but also describes JWT)
 * Body: TrackProductConsumptionRq
 * Response: ServiceResult<TrackProductConsumptionRs>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/subscription", produces = MediaType.APPLICATION_JSON_VALUE)
public class ConsumptionController {

    private final ConsumptionService service;

    @PostMapping(value = "/product-consumption/track", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResult<TrackProductConsumptionRs>> track(
            @RequestHeader("rqUID") final UUID rqUid,
            @RequestHeader("token") final String token,
            @Valid @RequestBody final TrackProductConsumptionRq body) {

        try {
            ServiceResult<TrackProductConsumptionRs> result =
                    service.trackProductConsumption(rqUid, token, body);
            return ServiceResultResponses.respond(result);
        } catch (ServiceResultException ex) {
            return ServiceResultResponses.respond(ex.getResult());
        }
    }
}
