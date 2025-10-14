package com.ejada.shared_starter_ratelimit;

import com.ejada.audit.starter.api.AuditAction;
import com.ejada.audit.starter.api.AuditEvent;
import com.ejada.audit.starter.api.AuditOutcome;
import com.ejada.audit.starter.api.AuditService;
import com.ejada.audit.starter.api.context.Actor;
import com.ejada.shared_starter_ratelimit.TokenBucketLuaRateLimiter.TokenBucketResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * Central coordinator for evaluating rate limit decisions.
 */
public class RateLimitService {

  private final RateLimitProps props;
  private final TenantRateLimitRegistry registry;
  private final RateLimitMetricsRecorder metricsRecorder;
  private final RateLimitKeyGenerator keyGenerator;
  private final TokenBucketLuaRateLimiter tokenBucket;
  private final RateLimitBypassEvaluator bypassEvaluator;
  private final AuditService auditService;

  public RateLimitService(RateLimitProps props,
      TenantRateLimitRegistry registry,
      RateLimitMetricsRecorder metricsRecorder,
      RateLimitKeyGenerator keyGenerator,
      TokenBucketLuaRateLimiter tokenBucket,
      RateLimitBypassEvaluator bypassEvaluator,
      AuditService auditService) {
    this.props = props;
    this.registry = registry;
    this.metricsRecorder = metricsRecorder;
    this.keyGenerator = keyGenerator;
    this.tokenBucket = tokenBucket;
    this.bypassEvaluator = bypassEvaluator;
    this.auditService = auditService;
  }

  public RateLimitDecision evaluate(RateLimitEvaluationRequest request) {
    RateLimitTier tier = registry.resolveTier(request.tenantId());
    Optional<RateLimitBypassDecision> bypass = bypassEvaluator.evaluate(request.authentication());
    if (bypass.isPresent()) {
      metricsRecorder.recordBypass(bypass.get().type());
      emitBypassAudit(request, bypass.get());
      return RateLimitDecision.bypass(bypass.get(), tier);
    }

    if (tier.requestsPerMinute() <= 0 || tier.burstCapacity() <= 0) {
      return RateLimitDecision.deny(RateLimitReason.QUOTA_EXCEEDED, tier, null, 0D, Duration.ZERO);
    }

    List<RateLimitProps.StrategyProperties> strategies = props.getMultidimensional().getStrategies();
    if (strategies == null || strategies.isEmpty()) {
      return RateLimitDecision.allow(tier, null, tier.burstCapacity());
    }
    double minRemaining = tier.burstCapacity();
    RateLimitStrategy limitingStrategy = null;
    for (RateLimitProps.StrategyProperties strategyProps : strategies) {
      if (!strategyProps.isEnabled()) {
        continue;
      }
      RateLimitStrategy strategy = new RateLimitStrategy(strategyProps.getName(), strategyProps.getDimensions());
      String key = keyGenerator.buildKey(strategy, request, tier);
      TokenBucketResponse response = tokenBucket.consume(key, tier);
      if (!response.allowed()) {
        metricsRecorder.recordRejected(tier.name(), strategy.name(), response.reason());
        return RateLimitDecision.deny(response.reason(), tier, strategy, response.remainingTokens(), response.retryAfter());
      }
      metricsRecorder.recordAllowed(tier.name(), strategy.name());
      limitingStrategy = strategy;
      minRemaining = Math.min(minRemaining, response.remainingTokens());
    }
    return RateLimitDecision.allow(tier, limitingStrategy, minRemaining);
  }

  private void emitBypassAudit(RateLimitEvaluationRequest request, RateLimitBypassDecision bypassDecision) {
    if (auditService == null || !props.getBypass().isAuditEnabled()) {
      return;
    }
    Authentication authentication = request.authentication();
    Actor actor = new Actor(request.safeUserId(),
        authentication != null ? authentication.getName() : request.safeUserId(),
        extractRoles(authentication),
        authentication != null ? authentication.getClass().getSimpleName() : null);

    AuditEvent event = AuditEvent.builder()
        .tenantId(request.safeTenantId())
        .action(AuditAction.ACCESS)
        .entity("RATE_LIMIT", request.safeTenantId())
        .outcome(AuditOutcome.SUCCESS)
        .actor(actor)
        .message("Rate limit bypassed")
        .meta("reason", bypassDecision.reasonCode())
        .meta("authority", bypassDecision.authority())
        .meta("ip", request.safeIpAddress())
        .put("endpoint", request.safeEndpoint())
        .build();
    auditService.emit(event);
  }

  private List<String> extractRoles(Authentication authentication) {
    if (authentication == null || authentication.getAuthorities() == null) {
      return List.of();
    }
    return authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .filter(Objects::nonNull)
        .collect(Collectors.toCollection(ArrayList::new));
  }
}
