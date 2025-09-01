package com.lms.tenant.api;

import com.lms.tenant.core.PolicyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/tenants/{tenantId}")
public class PolicyController {
  private final PolicyService service;

  public PolicyController(PolicyService service) {
    this.service = service;
  }

  @GetMapping("/entitlements")
  public ResponseEntity<PolicyService.Entitlements> entitlements(@PathVariable UUID tenantId) {
    return ResponseEntity.ok(service.effective(tenantId));
  }

  @PostMapping("/features/{featureKey}/consume")
  public ResponseEntity<PolicyService.EnforcementResult> consume(
      @PathVariable UUID tenantId,
      @PathVariable String featureKey,
      @Valid @RequestBody ConsumeRequest req) {
    var result = service.consumeOrOverage(
        tenantId,
        featureKey,
        req.delta(),
        req.periodStart(),
        req.periodEnd(),
        () -> req.currentUsage(),
        req.idempotencyKey());
    return ResponseEntity.ok(result);
  }

  public record ConsumeRequest(
      long delta,
      long currentUsage,
      @NotNull Instant periodStart,
      @NotNull Instant periodEnd,
      String idempotencyKey) {}
}
