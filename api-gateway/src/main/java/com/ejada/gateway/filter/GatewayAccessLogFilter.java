package com.ejada.gateway.filter;

import com.ejada.common.constants.HeaderNames;
import com.ejada.gateway.config.GatewayLoggingProperties;
import com.ejada.gateway.context.GatewayRequestAttributes;
import com.ejada.gateway.observability.GatewayTracingHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Emits structured JSON logs for all gateway ingress traffic suitable for ELK ingestion.
 */
public class GatewayAccessLogFilter implements WebFilter, Ordered {

  private static final Logger LOGGER = LoggerFactory.getLogger(GatewayAccessLogFilter.class);

  private final GatewayLoggingProperties loggingProperties;
  private final ObjectMapper objectMapper;
  private final AntPathMatcher pathMatcher = new AntPathMatcher();
  private final GatewayTracingHelper tracingHelper;

  public GatewayAccessLogFilter(GatewayLoggingProperties loggingProperties,
      ObjectMapper objectMapper,
      GatewayTracingHelper tracingHelper) {
    this.loggingProperties = loggingProperties;
    this.objectMapper = objectMapper;
    this.tracingHelper = tracingHelper;
  }

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE - 10;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    GatewayLoggingProperties.AccessLog accessLog = loggingProperties.getAccessLog();
    if (!accessLog.isEnabled()) {
      return chain.filter(exchange);
    }

    String path = exchange.getRequest().getPath().pathWithinApplication().value();
    if (shouldSkip(path, accessLog.getSkipPatterns())) {
      return chain.filter(exchange);
    }

    long start = System.nanoTime();
    return chain.filter(exchange)
        .doFinally(signalType -> {
          long durationMs = (System.nanoTime() - start) / 1_000_000;
          writeAccessLog(exchange, durationMs);
          tracingHelper.tagExchange(exchange);
        });
  }

  private boolean shouldSkip(String path, List<String> patterns) {
    if (patterns == null || patterns.isEmpty()) {
      return false;
    }
    for (String pattern : patterns) {
      if (StringUtils.hasText(pattern) && pathMatcher.match(pattern, path)) {
        return true;
      }
    }
    return false;
  }

  private void writeAccessLog(ServerWebExchange exchange, long durationMs) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("timestamp", Instant.now().toString());
    payload.put("correlationId", resolveCorrelationId(exchange));
    payload.put("tenantId", trimToNull(exchange.getAttribute(GatewayRequestAttributes.TENANT_ID)));
    payload.put("method", exchange.getRequest().getMethodValue());
    payload.put("path", exchange.getRequest().getPath().value());
    payload.put("statusCode", exchange.getResponse().getStatusCode() != null
        ? exchange.getResponse().getStatusCode().value()
        : 200);
    payload.put("duration", durationMs);
    payload.put("userAgent", exchange.getRequest().getHeaders().getFirst("User-Agent"));
    payload.put("clientIp", resolveClientIp(exchange.getRequest()));
    payload.put("routeId", resolveRouteId(exchange));

    try {
      LOGGER.info(objectMapper.writeValueAsString(payload));
    } catch (JsonProcessingException ex) {
      LOGGER.info("{}", payload, ex);
    }
  }

  private String resolveCorrelationId(ServerWebExchange exchange) {
    String attribute = exchange.getAttribute(GatewayRequestAttributes.CORRELATION_ID);
    if (StringUtils.hasText(attribute)) {
      return attribute;
    }
    String header = exchange.getRequest().getHeaders().getFirst(HeaderNames.CORRELATION_ID);
    return StringUtils.hasText(header) ? header : "unknown";
  }

  private String resolveClientIp(ServerHttpRequest request) {
    String forwarded = request.getHeaders().getFirst(HeaderNames.CLIENT_IP);
    if (!StringUtils.hasText(forwarded)) {
      forwarded = request.getHeaders().getFirst("X-Forwarded-For");
    }
    if (StringUtils.hasText(forwarded)) {
      return forwarded.split(",")[0].trim();
    }
    if (request.getRemoteAddress() != null) {
      return request.getRemoteAddress().getAddress().getHostAddress();
    }
    return "unknown";
  }

  private String resolveRouteId(ServerWebExchange exchange) {
    Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
    if (route == null || !StringUtils.hasText(route.getId())) {
      return "unknown";
    }
    return route.getId();
  }

  private String trimToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return value.trim();
  }
}
