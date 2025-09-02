package com.ejada.policy;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/tenants/{tenantId}/policy")
@Tag(name = "Policy")
public class PolicyController {

    private final PolicyService policyService;
    private final UsageReader usageReader;

    public PolicyController(PolicyService policyService, UsageReader usageReader) {
        this.policyService = policyService;
        this.usageReader = usageReader;
    }

    @PostMapping("/consume")
    @Operation(summary = "Consume feature usage and record overage if needed")
    public ConsumeResponse consume(@PathVariable UUID tenantId, @RequestBody ConsumeRequest request) {
        long usedBefore = usageReader.currentUsage(tenantId, request.featureKey(), request.periodStart(), request.periodEnd());
        long total = usedBefore + request.delta();
        var result = policyService.consumeOrOverage(tenantId, request.featureKey(), total);
        boolean overageRecorded = result.overageId() != null;
        boolean allowed = total <= result.limit() || overageRecorded;
        return new ConsumeResponse(allowed, request.featureKey(), result.limit(), usedBefore,
                request.delta(), overageRecorded, result.overageId());
    }

    public record ConsumeRequest(String featureKey,
                                 long delta,
                                 Instant periodStart,
                                 Instant periodEnd,
                                 String idempotencyKey) { }

    public record ConsumeResponse(boolean allowed,
                                  String featureKey,
                                  long limit,
                                  long usedBefore,
                                  long requestedDelta,
                                  boolean overageRecorded,
                                  UUID overageId) { }
}
