package com.ejada.gateway.observability;

import com.ejada.gateway.config.GatewayTracingProperties;
import com.ejada.gateway.context.GatewayRequestAttributes;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import java.time.Duration;
import java.util.Locale;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

/**
 * Convenience helper for enriching spans emitted by the gateway with additional
 * metadata and event annotations.
 */
public class GatewayTracingHelper {

  private final Tracer tracer;
  private final GatewayTracingProperties tracingProperties;

  public GatewayTracingHelper(@Nullable Tracer tracer, GatewayTracingProperties tracingProperties) {
    this.tracer = tracer;
    this.tracingProperties = tracingProperties;
  }

  public boolean isEnabled() {
    return tracer != null && tracingProperties != null && tracingProperties.isEnhancedTagsEnabled();
  }

  public void tagExchange(ServerWebExchange exchange) {
    if (!isEnabled()) {
      return;
    }
    Span span = tracer.currentSpan();
    if (span == null) {
      return;
    }
    String tenantId = trimToDefault(exchange.getAttribute(GatewayRequestAttributes.TENANT_ID));
    String tier = trimToDefault(exchange.getAttribute(GatewayRequestAttributes.SUBSCRIPTION_TIER));
    Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
    String routeId = route != null ? trimToDefault(route.getId()) : "unknown";
    span.tag("tenant_id", tenantId);
    span.tag("subscription_tier", tier);
    span.tag("route_id", routeId);
  }

  public void recordRateLimitDecision(ServerWebExchange exchange,
      boolean allowed,
      long remaining,
      long baseRemaining,
      long burstRemaining,
      Duration window,
      boolean burstConsumed,
      @Nullable String strategy) {
    if (!isEnabled()) {
      return;
    }
    Span span = tracer.currentSpan();
    if (span == null) {
      return;
    }
    String tenantId = trimToDefault(exchange.getAttribute(GatewayRequestAttributes.TENANT_ID));
    span.tag("rate_limit.strategy", trimToDefault(strategy));
    span.tag("rate_limit.allowed", Boolean.toString(allowed));
    span.event(String.format(Locale.ROOT,
        "rate_limit decision=%s tenant=%s remaining=%d baseRemaining=%d burstRemaining=%d burst=%s windowMs=%d",
        allowed ? "allowed" : "rejected",
        tenantId,
        remaining,
        baseRemaining,
        burstRemaining,
        burstConsumed,
        window != null ? window.toMillis() : -1));
  }

  public void recordSubscriptionValidation(ServerWebExchange exchange,
      Duration duration,
      boolean cacheHit,
      @Nullable String routeId,
      @Nullable String decision) {
    if (!isEnabled()) {
      return;
    }
    Span span = tracer.currentSpan();
    if (span == null) {
      return;
    }
    String tenantId = trimToDefault(exchange.getAttribute(GatewayRequestAttributes.TENANT_ID));
    String resolvedRoute = StringUtils.hasText(routeId) ? routeId : resolveRouteId(exchange);
    span.event(String.format(Locale.ROOT,
        "subscription.validation tenant=%s route=%s cacheHit=%s durationMs=%d decision=%s",
        tenantId,
        resolvedRoute,
        cacheHit,
        duration != null ? duration.toMillis() : -1,
        trimToDefault(decision)));
  }

  private String resolveRouteId(ServerWebExchange exchange) {
    Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
    if (route == null) {
      return "unknown";
    }
    return trimToDefault(route.getId());
  }

  private String trimToDefault(@Nullable String value) {
    if (!StringUtils.hasText(value)) {
      return "unknown";
    }
    return value.trim();
  }
}
