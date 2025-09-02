package com.ejada.subscription.web;

import com.ejada.subscription.core.SubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/tenants")
@Validated
public class SubscriptionController {

    private final SubscriptionService service;

    public SubscriptionController(SubscriptionService service) {
        this.service = service;
    }

    @GetMapping("/{tenantId}/subscription/active")
    public ResponseEntity<?> active(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(service.active(tenantId));
    }
}
