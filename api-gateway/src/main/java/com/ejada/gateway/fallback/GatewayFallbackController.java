package com.ejada.gateway.fallback;

import com.ejada.common.dto.BaseResponse;
import com.ejada.common.enums.StatusEnums.ApiStatus;
import com.ejada.gateway.config.GatewayRoutesProperties;
import com.ejada.gateway.config.GatewayRoutesProperties.ServiceRoute;
import com.ejada.gateway.config.GatewayRoutesProperties.ServiceRoute.Resilience;
import com.ejada.gateway.context.GatewayRequestAttributes;
import com.ejada.gateway.resilience.TenantCircuitBreakerMetrics;
import com.ejada.gateway.resilience.TenantCircuitBreakerMetrics.Priority;
import com.ejada.gateway.subscription.SubscriptionCacheService;
import com.ejada.gateway.subscription.SubscriptionRecord;
import com.ejada.gateway.fallback.CachedFallbackService.CachedFallbackContext;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Reactive handler that provides graceful degradation responses when
 * downstream services are temporarily unavailable and the circuit breaker
 * triggers.
 */
@RestController
@RequestMapping("/fallback")
public class GatewayFallbackController {

  private static final String DEFAULT_MESSAGE = "Downstream service is unavailable. Please retry shortly.";
  private static final String FALLBACK_METHOD_BLOCKED_MESSAGE =
      "Fallback is disabled for %s requests. Please retry once the service is healthy.";

  private final GatewayRoutesProperties properties;
  private final SubscriptionCacheService subscriptionCacheService;
  private final BillingFallbackQueue billingFallbackQueue;
  private final CachedFallbackService cachedFallbackService;
  private final TenantCircuitBreakerMetrics circuitBreakerMetrics;

  public GatewayFallbackController(GatewayRoutesProperties properties,
      SubscriptionCacheService subscriptionCacheService,
      BillingFallbackQueue billingFallbackQueue,
      CachedFallbackService cachedFallbackService,
      TenantCircuitBreakerMetrics circuitBreakerMetrics) {
    this.properties = properties;
    this.subscriptionCacheService = subscriptionCacheService;
    this.billingFallbackQueue = billingFallbackQueue;
    this.cachedFallbackService = cachedFallbackService;
    this.circuitBreakerMetrics = circuitBreakerMetrics;
  }

  @RequestMapping("/{routeId}")
  public Mono<ResponseEntity<BaseResponse<FallbackResponse>>> fallback(@PathVariable String routeId,
      ServerWebExchange exchange) {
    Optional<ServiceRoute> routeOptional = properties.findRouteById(routeId);
    Resilience resilience = routeOptional.map(ServiceRoute::getResilience).orElse(null);

    HttpStatus status = (resilience != null && resilience.getFallbackStatus() != null)
        ? resilience.getFallbackStatus()
        : HttpStatus.SERVICE_UNAVAILABLE;
    String message = Optional.ofNullable(resilience)
        .flatMap(res -> res.resolvedFallbackMessage())
        .filter(StringUtils::hasText)
        .orElse(DEFAULT_MESSAGE);
    HttpMethod method = exchange.getRequest().getMethod();
    if (resilience != null && !resilience.isFallbackMethodAllowed(method)) {
      return Mono.just(fallbackNotAllowedResponse(method));
    }
    String tenantId = exchange.getAttribute(GatewayRequestAttributes.TENANT_ID);
    String circuitBreakerName = Optional.ofNullable(resilience)
        .map(res -> res.resolvedCircuitBreakerName(routeId))
        .orElse(routeId);
    Priority priority = Optional.ofNullable(resilience)
        .map(Resilience::getPriority)
        .map(value -> Priority.valueOf(value.name()))
        .orElse(Priority.NON_CRITICAL);

    Mono<ResponseEntity<BaseResponse<FallbackResponse>>> routeSpecific = switch (routeId) {
      case "catalog-service" -> catalogFallback(routeId, circuitBreakerName, tenantId, status, message,
          exchange, priority);
      case "billing-service" -> billingFallback(routeId, circuitBreakerName, tenantId, status, message,
          exchange, priority);
      case "subscription-service" -> subscriptionFallback(routeId, circuitBreakerName, tenantId, status,
          message, exchange, priority);
      case "tenant-service" -> tenantServiceFallback(routeId, circuitBreakerName, tenantId, status, message,
          exchange, priority);
      default -> Mono.just(defaultFallback(routeId, circuitBreakerName, tenantId, status, message, priority));
    };

    if (priority == Priority.CRITICAL && !"tenant-service".equals(routeId)) {
      return cachedFallbackService.resolve(routeId, exchange)
          .map(context -> buildCachedFallbackResponse(routeId, circuitBreakerName, tenantId, status, message,
              priority, context))
          .switchIfEmpty(routeSpecific);
    }
    return routeSpecific;
  }

  private Mono<ResponseEntity<BaseResponse<FallbackResponse>>> catalogFallback(String routeId,
      String circuitBreakerName,
      String tenantId,
      HttpStatus status,
      String message,
      ServerWebExchange exchange,
      Priority priority) {
    return subscriptionCacheService.getCached(tenantId)
        .flatMap(optional -> optional
            .map(record -> Mono.just(buildCatalogFallbackResponse(routeId, circuitBreakerName, tenantId, status,
                message, exchange, record, priority)))
            .orElseGet(() -> Mono.just(defaultFallback(routeId, circuitBreakerName, tenantId,
                HttpStatus.SERVICE_UNAVAILABLE, DEFAULT_MESSAGE, priority))));
  }

  private Mono<ResponseEntity<BaseResponse<FallbackResponse>>> tenantServiceFallback(String routeId,
      String circuitBreakerName,
      String tenantId,
      HttpStatus status,
      String message,
      ServerWebExchange exchange,
      Priority priority) {
    if (priority != Priority.CRITICAL) {
      return Mono.just(defaultFallback(routeId, circuitBreakerName, tenantId, status, message, priority));
    }
    return cachedFallbackService.resolve(routeId, exchange)
        .map(context -> buildCachedFallbackResponse(routeId, circuitBreakerName, tenantId, status, message,
            priority, context))
        .switchIfEmpty(Mono.just(defaultFallback(routeId, circuitBreakerName, tenantId, status, message, priority)));
  }

  private ResponseEntity<BaseResponse<FallbackResponse>> buildCatalogFallbackResponse(String routeId,
      String circuitBreakerName,
      String tenantId,
      HttpStatus status,
      String message,
      ServerWebExchange exchange,
      SubscriptionRecord record,
      Priority priority) {
    String rawTier = Optional.ofNullable(exchange.getAttribute(GatewayRequestAttributes.SUBSCRIPTION_TIER))
        .map(Object::toString)
        .orElse(null);
    String tier = StringUtils.hasText(rawTier) ? rawTier.trim() : "unknown";
    CatalogFallbackPayload payload = new CatalogFallbackPayload(tier,
        record != null ? record.enabledFeatures() : Set.of(),
        record != null ? record.fetchedAt() : Instant.now());
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("strategy", "catalog-cache");
    metadata.put("priority", priority.name());
    metadata.put("tenantId", Optional.ofNullable(tenantId).orElse("unknown"));
    recordFallback(circuitBreakerName, tenantId, "CATALOG_CACHE", metadata);

    return buildResponse(status, ApiStatus.WARNING, status.name(), message,
        new FallbackResponse(routeId, message, Instant.now(), payload, metadata), headers -> {
          headers.add("X-Catalog-Fallback", "cached-tier");
          headers.add("X-Catalog-Tier", tier);
          if (record != null && record.fetchedAt() != null) {
            headers.add("X-Catalog-Cache-Timestamp", record.fetchedAt().toString());
          }
        });
  }

  private Mono<ResponseEntity<BaseResponse<FallbackResponse>>> billingFallback(String routeId,
      String circuitBreakerName,
      String tenantId,
      HttpStatus status,
      String message,
      ServerWebExchange exchange,
      Priority priority) {
    return billingFallbackQueue.enqueue(tenantId, exchange.getRequest())
        .map(queueKey -> {
          String method = Optional.ofNullable(exchange.getRequest().getMethod())
              .map(HttpMethod::name)
              .orElse("UNKNOWN");
          BillingFallbackPayload payload = new BillingFallbackPayload(queueKey, Instant.now(),
              exchange.getRequest().getURI().getPath(), method);
          Map<String, Object> metadata = new LinkedHashMap<>();
          metadata.put("strategy", "billing-queue");
          metadata.put("priority", priority.name());
          metadata.put("tenantId", Optional.ofNullable(tenantId).orElse("unknown"));
          metadata.put("queueKey", queueKey);
          recordFallback(circuitBreakerName, tenantId, "BILLING_QUEUED", metadata);
          return buildResponse(status, ApiStatus.SUCCESS, status.name(), message,
              new FallbackResponse(routeId, message, Instant.now(), payload, metadata), headers -> {
                headers.add("X-Billing-Fallback", "queued");
                headers.add("X-Billing-Queue", queueKey);
              });
        })
        .onErrorReturn(defaultFallback(routeId, circuitBreakerName, tenantId,
            HttpStatus.SERVICE_UNAVAILABLE, DEFAULT_MESSAGE, priority));
  }

  private Mono<ResponseEntity<BaseResponse<FallbackResponse>>> subscriptionFallback(String routeId,
      String circuitBreakerName,
      String tenantId,
      HttpStatus status,
      String message,
      ServerWebExchange exchange,
      Priority priority) {
    return subscriptionCacheService.getCached(tenantId)
        .flatMap(optional -> optional
            .map(record -> Mono.just(buildSubscriptionFallbackResponse(routeId, circuitBreakerName, tenantId,
                status, message, exchange, record, priority)))
            .orElseGet(() -> Mono.just(defaultFallback(routeId, circuitBreakerName, tenantId,
                HttpStatus.SERVICE_UNAVAILABLE, DEFAULT_MESSAGE, priority))));
  }

  private ResponseEntity<BaseResponse<FallbackResponse>> buildSubscriptionFallbackResponse(String routeId,
      String circuitBreakerName,
      String tenantId,
      HttpStatus status,
      String message,
      ServerWebExchange exchange,
      SubscriptionRecord record,
      Priority priority) {
    SubscriptionFallbackPayload payload = SubscriptionFallbackPayload.fromRecord(record);
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("strategy", "subscription-stale");
    metadata.put("priority", priority.name());
    metadata.put("tenantId", Optional.ofNullable(tenantId).orElse("unknown"));
    metadata.put("staleSince", payload.cachedAt().toString());
    recordFallback(circuitBreakerName, tenantId, "SUBSCRIPTION_STALE", metadata);

    return buildResponse(status, ApiStatus.WARNING, status.name(), message,
        new FallbackResponse(routeId, message, Instant.now(), payload, metadata), headers -> {
          headers.add("X-Subscription-Fallback", "stale-cache");
          headers.add("X-Subscription-Stale-Since", payload.cachedAt().toString());
          if (payload.cachedAt() != null) {
            Duration staleFor = Duration.between(payload.cachedAt(), Instant.now());
            headers.add("X-Subscription-Stale-For",
                Long.toString(Math.max(0, staleFor.toSeconds())));
          }
        });
  }

  private ResponseEntity<BaseResponse<FallbackResponse>> buildCachedFallbackResponse(String routeId,
      String circuitBreakerName,
      String tenantId,
      HttpStatus status,
      String message,
      Priority priority,
      CachedFallbackContext context) {
    Map<String, Object> metadata = new LinkedHashMap<>(context.metadata());
    metadata.put("strategy", "redis-cache");
    metadata.put("priority", priority.name());
    metadata.put("tenantId", Optional.ofNullable(tenantId).orElse("unknown"));
    metadata.put("cacheState", context.cacheState().name());
    recordFallback(circuitBreakerName, tenantId, "REDIS_CACHE", metadata);
    CachedFallbackPayload payload = context.payload();
    Instant now = Instant.now();
    return buildResponse(status, ApiStatus.WARNING, status.name(), message,
        new FallbackResponse(routeId, message, now, payload, metadata), headers -> {
          headers.add("X-Fallback-Source", "redis-cache");
          headers.add("X-Fallback-Cache-Key", context.cacheKey());
          headers.add("X-Fallback-Cache-State", context.cacheState().name());
          headers.add("X-Fallback-Cache-Captured-At", payload.cachedAt().toString());
          headers.add("X-Fallback-Cache-Expires-At", payload.expiresAt().toString());
          headers.add("X-Fallback-Cache-Stale-At", payload.staleAt().toString());
          Duration age = Duration.between(payload.cachedAt(), now);
          headers.add("X-Fallback-Cache-Age", Long.toString(Math.max(0, age.toSeconds())));
          if (payload.staleAt().isBefore(now)) {
            headers.add("X-Fallback-Cache-Stale-For",
                Long.toString(Math.max(0, Duration.between(payload.staleAt(), now).toSeconds())));
          } else {
            headers.add("X-Fallback-Cache-Stale-In",
                Long.toString(Math.max(0, Duration.between(now, payload.staleAt()).toSeconds())));
          }
        });
  }

  private ResponseEntity<BaseResponse<FallbackResponse>> defaultFallback(String routeId,
      String circuitBreakerName,
      String tenantId,
      HttpStatus status,
      String message,
      Priority priority) {
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("strategy", "default");
    metadata.put("priority", priority.name());
    metadata.put("tenantId", Optional.ofNullable(tenantId).orElse("unknown"));
    recordFallback(circuitBreakerName, tenantId, "DEFAULT", metadata);
    return buildResponse(status, ApiStatus.ERROR, status.name(), message,
        new FallbackResponse(routeId, message, Instant.now(), null, metadata), null);
  }

  private ResponseEntity<BaseResponse<FallbackResponse>> buildResponse(HttpStatus httpStatus,
      ApiStatus apiStatus,
      String code,
      String message,
      FallbackResponse payload,
      java.util.function.Consumer<org.springframework.http.HttpHeaders> headersCustomizer) {
    BaseResponse<FallbackResponse> envelope = BaseResponse.<FallbackResponse>builder()
        .status(apiStatus)
        .code(code)
        .message(message)
        .data(payload)
        .build();
    BodyBuilder builder = ResponseEntity.status(httpStatus);
    if (headersCustomizer != null) {
      org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
      headersCustomizer.accept(headers);
      builder.headers(headers);
    }
    return builder.body(envelope);
  }

  private void recordFallback(String circuitBreakerName,
      String tenantId,
      String fallbackType,
      Map<String, Object> metadata) {
    circuitBreakerMetrics.recordFallback(circuitBreakerName, tenantId, fallbackType, metadata);
  }

  private ResponseEntity<BaseResponse<FallbackResponse>> fallbackNotAllowedResponse(HttpMethod method) {
    String resolvedMethod = method != null ? method.name() : "UNKNOWN";
    BaseResponse<FallbackResponse> envelope = BaseResponse.<FallbackResponse>builder()
        .status(ApiStatus.ERROR)
        .code("ERR_FALLBACK_NOT_ALLOWED")
        .message(String.format(FALLBACK_METHOD_BLOCKED_MESSAGE, resolvedMethod))
        .build();
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(envelope);
  }
}
