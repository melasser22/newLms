package com.ejada.billing.controller;

import com.ejada.billing.dto.ProductSubscriptionStts;
import com.ejada.billing.service.ConsumptionQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/billing")
public final class ConsumptionQueryController {

    private final ConsumptionQueryService service;

    /** Returns TRANSACTION/USER/BALANCE snapshot for a subscription. */
    @GetMapping("/subscriptions/{extSubscriptionId}/consumption")
    public ResponseEntity<ProductSubscriptionStts> getConsumption(
            @PathVariable final Long extSubscriptionId,
            @RequestParam(required = false) final Long customerId) {

        return ResponseEntity.ok(service.getSnapshot(extSubscriptionId, customerId));
    }
}
