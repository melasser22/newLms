package com.ejada.gateway.filter;

import com.ejada.common.dto.BaseResponse;
import com.ejada.common.enums.StatusEnums;
import com.ejada.gateway.config.SubscriptionValidationProperties;
import com.ejada.gateway.context.GatewayRequestAttributes;
import com.ejada.gateway.observability.GatewayTracingHelper;
import com.ejada.gateway.subscription.SubscriptionCacheService;
import com.ejada.gateway.subscription.SubscriptionRecord;
import com.ejada.gateway.subscription.SubscriptionUsageTracker;
import com.ejada.gateway.subscription.SubscriptionUsageTracker.UsageCheck;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Validates tenant subscriptions before forwarding traffic to downstream services.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 40)
public class SubscriptionValidationGatewayFilter implements GlobalFilter, Ordered {

  private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionValidationGatewayFilter.class);

  private static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();
  private static final String GRACE_HEADER = "X-Subscription-Grace-Remaining";

  private final SubscriptionValidationProperties properties;
  private final SubscriptionCacheService cacheService;
  private final SubscriptionUsageTracker usageTracker;
  private final ObjectMapper objectMapper;
  private final MeterRegistry meterRegistry;
  private final GatewayTracingHelper tracingHelper;

  public SubscriptionValidationGatewayFilter(
      SubscriptionValidationProperties properties,
      SubscriptionCacheService cacheService,
      SubscriptionUsageTracker usageTracker,
      @Qualifier("jacksonObjectMapper") ObjectProvider<ObjectMapper> primaryObjectMapper,
      ObjectProvider<ObjectMapper> fallbackObjectMapper,
      MeterRegistry meterRegistry,
      GatewayTracingHelper tracingHelper) {
    this.properties = Objects.requireNonNull(properties, "properties");
    this.cacheService = Objects.requireNonNull(cacheService, "cacheService");
    this.usageTracker = Objects.requireNonNull(usageTracker, "usageTracker");
    ObjectMapper mapper = (primaryObjectMapper != null) ? primaryObjectMapper.getIfAvailable() : null;
    if (mapper == null) {
      mapper = (fallbackObjectMapper != null) ? fallbackObjectMapper.getIfAvailable() : null;
    }
    this.objectMapper = Objects.requireNonNull(mapper, "objectMapper");
    this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry");
    this.tracingHelper = Objects.requireNonNull(tracingHelper, "tracingHelper");
  }

  @Override
  @Timed(value = "gateway.subscription.validation", description = "Subscription validation latency")
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    if (!properties.isEnabled()) {
      return chain.filter(exchange);
    }

    String path = exchange.getRequest().getPath().value();
    if (shouldSkip(path)) {
      return chain.filter(exchange);
    }

    String tenantId = exchange.getAttribute(GatewayRequestAttributes.TENANT_ID);
    if (!StringUtils.hasText(tenantId)) {
      return chain.filter(exchange);
    }

    Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
    String routeId = route != null ? route.getId() : null;
    if (!properties.requiresValidation(routeId)) {
      return chain.filter(exchange);
    }

    Set<String> requiredFeatures = properties.requiredFeaturesFor(routeId);

    long start = System.nanoTime();
    AtomicBoolean cacheHit = new AtomicBoolean(false);

    return ensureSubscription(tenantId, requiredFeatures, exchange, cacheHit)
        .flatMap(decision -> {
          tracingHelper.recordSubscriptionValidation(exchange,
              Duration.ofNanos(System.nanoTime() - start),
              cacheHit.get(),
              routeId,
              decision.reason() != null ? decision.reason().name() : "unknown");
          recordOutcomeMetrics(tenantId, decision);
          if (decision.allowed()) {
            if (decision.reason() == DecisionReason.GRACE_ALLOWED && decision.graceRemaining() != null) {
              exchange.getResponse().getHeaders().set(GRACE_HEADER,
                  String.valueOf(decision.graceRemaining().getSeconds()));
              meterRegistry.counter("gateway.subscription.grace_period_used", "tenantId", tenantId)
                  .increment();
            }
            exchange.getAttributes().put(GatewayRequestAttributes.SUBSCRIPTION, decision.record());
            String tier = resolveTier(decision.record());
            if (StringUtils.hasText(tier)) {
              exchange.getAttributes().put(GatewayRequestAttributes.SUBSCRIPTION_TIER, tier);
            }
            return chain.filter(exchange);
          }
          return reject(exchange, decision);
        });
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 40;
  }

  private boolean shouldSkip(String path) {
    for (String pattern : properties.getSkipPatterns()) {
      if (StringUtils.hasText(pattern) && ANT_PATH_MATCHER.match(pattern, path)) {
        return true;
      }
    }
    return false;
  }

  private Mono<SubscriptionDecision> ensureSubscription(String tenantId, Set<String> requiredFeatures,
      ServerWebExchange exchange, AtomicBoolean cacheHit) {
    return cacheService.getCached(tenantId)
        .doOnNext(optional -> optional.ifPresent(ignored -> cacheHit.set(true)))
        .flatMap(optional -> optional.map(record -> Mono.just(new SubscriptionContext(record, true)))
            .orElseGet(Mono::empty))
        .switchIfEmpty(cacheService.fetchAndCache(tenantId).map(record -> new SubscriptionContext(record, false)))
        .flatMap(context -> evaluateAndEnforce(tenantId, requiredFeatures, exchange, context))
        .onErrorResume(ex -> {
          LOGGER.warn("Subscription validation failed for tenant {}", tenantId, ex);
          if (properties.isFailOpen()) {
            return Mono.just(SubscriptionDecision.allow(null, DecisionReason.ERROR));
          }
          return Mono.just(SubscriptionDecision.deny(HttpStatus.SERVICE_UNAVAILABLE,
              "ERR_SUBSCRIPTION_UNAVAILABLE",
              "Unable to validate subscription status",
              DecisionReason.ERROR,
              Duration.ZERO,
              null,
              null));
        });
  }

  private Mono<SubscriptionDecision> evaluateAndEnforce(String tenantId, Set<String> requiredFeatures,
      ServerWebExchange exchange, SubscriptionContext context) {
    SubscriptionDecision decision = evaluate(context.record(), requiredFeatures, exchange);
    if (decision.allowed()) {
      return enforceUsage(tenantId, requiredFeatures, context.record(), decision);
    }
    if (decision.reason() == DecisionReason.MISSING_FEATURE && context.fromCache()) {
      return cacheService.fetchAndCache(tenantId)
          .map(record -> new SubscriptionContext(record, false))
          .flatMap(refreshed -> {
            SubscriptionDecision refreshedDecision = evaluate(refreshed.record(), requiredFeatures, exchange);
            if (refreshedDecision.allowed()) {
              return enforceUsage(tenantId, requiredFeatures, refreshed.record(), refreshedDecision);
            }
            return Mono.just(refreshedDecision);
          });
    }
    return Mono.just(decision);
  }

  private Mono<SubscriptionDecision> enforceUsage(String tenantId, Set<String> requiredFeatures,
      SubscriptionRecord record, SubscriptionDecision decision) {
    if (record == null || requiredFeatures.isEmpty()) {
      return Mono.just(decision);
    }
    return Flux.fromIterable(requiredFeatures)
        .concatMap(feature -> usageTracker.recordUsage(tenantId, feature, record))
        .filter(UsageCheck::exceeded)
        .next()
        .map(check -> {
          meterRegistry.counter("gateway.subscription.quota_exceeded",
              "tenantId", tenantId,
              "feature", Optional.ofNullable(check.feature()).orElse("unknown"))
              .increment();
          Map<String, Object> details = new LinkedHashMap<>();
          details.put("feature", check.feature());
          details.put("limit", check.limit());
          details.put("usage", check.usage());
          if (record != null && StringUtils.hasText(record.upgradeUrl())) {
            details.put("upgradeUrl", record.upgradeUrl());
          }
          String message = String.format("Feature quota exceeded for %s", check.feature());
          return SubscriptionDecision.deny(HttpStatus.TOO_MANY_REQUESTS,
              "ERR_FEATURE_QUOTA_EXCEEDED",
              message,
              DecisionReason.FEATURE_QUOTA,
              Duration.ZERO,
              details,
              record);
        })
        .defaultIfEmpty(decision);

  }


  private SubscriptionDecision evaluate(@Nullable SubscriptionRecord record, Set<String> requiredFeatures,
      ServerWebExchange exchange) {
    if (record == null) {
      return SubscriptionDecision.deny(HttpStatus.SERVICE_UNAVAILABLE,
          "ERR_SUBSCRIPTION_UNAVAILABLE",
          "Subscription details unavailable",
          DecisionReason.ERROR,
          Duration.ZERO,
          null,
          null);
    }
    Duration gracePeriod = properties.getGracePeriod();
    if (!record.isActive()) {
      if (record.isWithinGrace(gracePeriod)) {
        Duration remaining = record.graceRemaining(gracePeriod);
        if (isReadOnly(exchange)) {
          return SubscriptionDecision.allowWithGrace(record, remaining);
        }
        Map<String, Object> details = upgradeDetails(record);
        details.put("graceRemaining", remaining);
        return SubscriptionDecision.deny(HttpStatus.PAYMENT_REQUIRED,
            "ERR_SUBSCRIPTION_GRACE_READONLY",
            "Subscription expired. Write operations disabled during grace period",
            DecisionReason.GRACE_WRITE_BLOCKED,
            remaining,
            details,
            record);
      }
      return SubscriptionDecision.deny(HttpStatus.PAYMENT_REQUIRED,
          "ERR_SUBSCRIPTION_INACTIVE",
          "Subscription is inactive or expired",
          DecisionReason.INACTIVE,
          Duration.ZERO,
          upgradeDetails(record),
          record);
    }

    Set<String> missing = requiredFeatures.stream()
        .filter(feature -> !record.hasFeature(feature))
        .collect(Collectors.toCollection(LinkedHashSet::new));
    if (!missing.isEmpty()) {
      Map<String, Object> details = upgradeDetails(record);
      details.put("missingFeatures", missing);
      String message = "Subscription no longer includes required features: " + String.join(", ", missing);
      return SubscriptionDecision.deny(HttpStatus.PAYMENT_REQUIRED,
          "ERR_SUBSCRIPTION_FEATURE",
          message,
          DecisionReason.MISSING_FEATURE,
          Duration.ZERO,
          details,
          record);
    }

    return SubscriptionDecision.allow(record, DecisionReason.ALLOWED);
  }

  private boolean isReadOnly(ServerWebExchange exchange) {
    HttpMethod method = exchange.getRequest().getMethod();
    return method == HttpMethod.GET || method == HttpMethod.HEAD || method == HttpMethod.OPTIONS;
  }

  private Map<String, Object> upgradeDetails(SubscriptionRecord record) {
    Map<String, Object> details = new LinkedHashMap<>();
    if (record != null && StringUtils.hasText(record.upgradeUrl())) {
      details.put("upgradeUrl", record.upgradeUrl());
    }
    return details;
  }

  private Mono<Void> reject(ServerWebExchange exchange, SubscriptionDecision decision) {
    exchange.getResponse().setStatusCode(decision.status());
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
    BaseResponse<Object> body = BaseResponse.<Object>builder()
        .status(StatusEnums.ApiStatus.ERROR)
        .code(decision.code())
        .message(decision.message())
        .data(decision.details())
        .build();
    byte[] payload;
    try {
      payload = objectMapper.writeValueAsBytes(body);
    } catch (JsonProcessingException e) {
      payload = body.toString().getBytes(StandardCharsets.UTF_8);
    }
    return exchange.getResponse()
        .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(payload)));
  }

  private void recordOutcomeMetrics(String tenantId, SubscriptionDecision decision) {
    String resolvedTenant = StringUtils.hasText(tenantId) ? tenantId : "unknown";
    String reason = decision.reason() != null ? decision.reason().name() : "UNKNOWN";
    meterRegistry.counter("gateway.subscription.validation.outcomes",
            "tenantId", resolvedTenant,
            "outcome", decision.allowed() ? "success" : "failure",
            "reason", reason)
        .increment();
    if (!decision.allowed()) {
      meterRegistry.counter("gateway.subscription.validation.failures",
              "tenantId", resolvedTenant,
              "reason", reason)
          .increment();
    }
  }

  private record SubscriptionContext(SubscriptionRecord record, boolean fromCache) {
  }

  private record SubscriptionDecision(boolean allowed,
                                       @Nullable SubscriptionRecord record,
                                       HttpStatus status,
                                       String code,
                                       String message,
                                       DecisionReason reason,
                                       Duration graceRemaining,
                                       Map<String, Object> details) {

    static SubscriptionDecision allow(@Nullable SubscriptionRecord record, DecisionReason reason) {
      return new SubscriptionDecision(true, record, HttpStatus.OK, null, null, reason, Duration.ZERO, null);
    }

    static SubscriptionDecision allowWithGrace(SubscriptionRecord record, Duration remaining) {
      return new SubscriptionDecision(true, record, HttpStatus.OK, null, null,
          DecisionReason.GRACE_ALLOWED, remaining, null);

    }

    static SubscriptionDecision deny(HttpStatus status, String code, String message, DecisionReason reason,
        Duration graceRemaining, Map<String, Object> details, @Nullable SubscriptionRecord record) {
      return new SubscriptionDecision(false, record, status, code, message, reason, graceRemaining, details);
    }
  }

  private enum DecisionReason {
    ALLOWED,
    GRACE_ALLOWED,
    INACTIVE,
    MISSING_FEATURE,
    GRACE_WRITE_BLOCKED,
    FEATURE_QUOTA,
    ERROR

  }

  private static final Pattern TIER_PATTERN = Pattern.compile("(?i)tier[:/_-]?([a-z0-9]+)");

  private String resolveTier(@Nullable SubscriptionRecord record) {
    if (record == null) {
      return null;
    }
    for (String feature : Optional.ofNullable(record.enabledFeatures()).orElse(Set.of())) {
      String tier = extractTier(feature);
      if (tier != null) {
        return tier;
      }
    }
    if (record.allocations() != null) {
      for (String key : record.allocations().keySet()) {
        String tier = extractTier(key);
        if (tier != null) {
          return tier;
        }
      }
    }
    return null;
  }

  private String extractTier(String candidate) {
    if (!StringUtils.hasText(candidate)) {
      return null;
    }
    Matcher matcher = TIER_PATTERN.matcher(candidate.trim());
    if (matcher.matches() || matcher.find()) {
      return matcher.group(1).toLowerCase();
    }
    return null;
  }
}
