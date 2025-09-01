package com.lms.tenant.subscription.web;

import com.lms.tenant.subscription.service.SubscriptionService;
import com.shared.subscription.api.SubscriptionDto;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tenants/{tenantId}/subscription")
public class SubscriptionController {

    private final SubscriptionService service;

    public SubscriptionController(SubscriptionService service) {
        this.service = service;
    }

    @PutMapping("/trial")
    public ResponseEntity<SubscriptionDto> startTrial(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(service.startTrial(tenantId));
    }

    @PutMapping
    public ResponseEntity<SubscriptionDto> activate(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(service.activate(tenantId));
    }

    @PutMapping("/{subscriptionId}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable UUID tenantId, @PathVariable UUID subscriptionId) {
        service.cancel(tenantId, subscriptionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/active")
    public ResponseEntity<SubscriptionDto> active(@PathVariable UUID tenantId) {
        return service.findActiveSubscription(tenantId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
