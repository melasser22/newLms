package com.lms.tenant.api;

import com.lms.tenant.core.PolicyService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;

@RestController
@RequestMapping("/tenants/{tenantId}")
@Validated
public class PolicyController {
  private final PolicyService service;

  public PolicyController(PolicyService service) {
    this.service = service;
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
        () -> req.currentUsage(),
        req.idempotencyKey());
    return ResponseEntity.ok(result);
  }

  public record ConsumeRequest(
      @Positive long delta,
      @PositiveOrZero long currentUsage,
      String idempotencyKey) {}
}
