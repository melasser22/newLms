package com.ejada.billing.controller;

import com.ejada.billing.dto.ProductSubscriptionStts;
import com.ejada.billing.service.ConsumptionQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/billing")
public class ConsumptionQueryController {

    private final ConsumptionQueryService service;

    /** Returns TRANSACTION/USER/BALANCE snapshot for a subscription. */
    @GetMapping("/subscriptions/{extSubscriptionId}/consumption")
    public ResponseEntity<ProductSubscriptionStts> getConsumption(
            @PathVariable Long extSubscriptionId,
            @RequestParam(required = false) Long customerId) {

        return ResponseEntity.ok(service.getSnapshot(extSubscriptionId, customerId));
    }
}
