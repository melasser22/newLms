package com.ejada.billing.controller;

import com.ejada.billing.dto.ServiceResult;
import com.ejada.billing.dto.TrackProductConsumptionRq;
import com.ejada.billing.dto.TrackProductConsumptionRs;
import com.ejada.billing.service.ConsumptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            @RequestHeader("rqUID") UUID rqUid,
            @RequestHeader("token") String token,
            @Valid @RequestBody TrackProductConsumptionRq body) {

        return ResponseEntity.ok(service.trackProductConsumption(rqUid, token, body));
    }
}
