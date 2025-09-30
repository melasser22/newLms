package com.ejada.billing.controller;

import com.ejada.billing.dto.ServiceResult;
import com.ejada.billing.dto.TrackProductConsumptionRq;
import com.ejada.billing.dto.TrackProductConsumptionRs;
import com.ejada.billing.exception.ServiceResultException;
import com.ejada.billing.service.ConsumptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
            return respond(result);
        } catch (ServiceResultException ex) {
            return respond(ex.getResult());
        }
    }

    private ResponseEntity<ServiceResult<TrackProductConsumptionRs>> respond(
            final ServiceResult<TrackProductConsumptionRs> result) {

        if (result == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
        HttpStatus status = Boolean.TRUE.equals(result.success())
                ? HttpStatus.OK
                : HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status).body(result);
    }
}
