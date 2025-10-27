package com.ejada.gateway.filter;

import com.ejada.common.constants.HeaderNames;
import com.ejada.gateway.config.GatewayLoggingProperties;
import com.ejada.gateway.context.GatewayRequestAttributes;
import com.ejada.gateway.observability.GatewayTracingHelper;
import com.ejada.gateway.routes.model.RouteCallAuditRecord;
import com.ejada.gateway.routes.service.RouteCallAuditService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;

/**
 * Emits structured JSON logs for all gateway ingress traffic suitable for ELK ingestion.
 */
public class GatewayAccessLogFilter implements WebFilter, Ordered {

  private static final Logger LOGGER = LoggerFactory.getLogger(GatewayAccessLogFilter.class);

  private final GatewayLoggingProperties loggingProperties;
  private final ObjectMapper objectMapper;
  private final AntPathMatcher pathMatcher = new AntPathMatcher();
  private final GatewayTracingHelper tracingHelper;
  private final RouteCallAuditService routeCallAuditService;

  public GatewayAccessLogFilter(GatewayLoggingProperties loggingProperties,
      ObjectMapper objectMapper,
      GatewayTracingHelper tracingHelper,
      RouteCallAuditService routeCallAuditService) {
    this.loggingProperties = loggingProperties;
    this.objectMapper = objectMapper;
    this.tracingHelper = tracingHelper;
    this.routeCallAuditService = routeCallAuditService;
  }

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE - 10;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    GatewayLoggingProperties.AccessLog accessLog = loggingProperties.getAccessLog();
    String path = exchange.getRequest().getPath().pathWithinApplication().value();
    if (shouldSkip(path, accessLog.getSkipPatterns())) {
      return chain.filter(exchange);
    }

    boolean accessLogEnabled = accessLog.isEnabled();
    long start = System.nanoTime();
    return chain.filter(exchange)
        .materialize()
        .flatMap(signal -> {
          long durationMs = (System.nanoTime() - start) / 1_000_000;
          if (accessLogEnabled) {
            writeAccessLog(exchange, durationMs);
            tracingHelper.tagExchange(exchange);
          }
          return routeCallAuditService.record(buildAuditRecord(exchange, durationMs, signal))
              .doOnError(ex ->
                  LOGGER.warn(
                      "Failed to audit route call for {}", exchange.getRequest().getPath(), ex))
              .then(signal.isOnError() ? Mono.error(signal.getThrowable()) : Mono.<Void>empty());
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
    String method = Optional.ofNullable(exchange.getRequest().getMethod())
        .map(HttpMethod::name)
        .orElse("UNKNOWN");
    payload.put("method", method);
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

  private RouteCallAuditRecord buildAuditRecord(ServerWebExchange exchange, long durationMs, Signal<Void> signal) {
    String routeId = resolveRouteId(exchange);
    String method = Optional.ofNullable(exchange.getRequest().getMethod())
        .map(HttpMethod::name)
        .orElse("UNKNOWN");
    int status = Optional.ofNullable(exchange.getResponse().getStatusCode())
        .map(code -> code.value())
        .orElseGet(() -> signal.isOnError() ? 500 : 200);
    String errorMessage = signal.getThrowable() != null ? signal.getThrowable().getMessage() : null;
    return new RouteCallAuditRecord(
        routeId,
        exchange.getRequest().getPath().value(),
        method,
        status,
        durationMs,
        trimToNull(exchange.getAttribute(GatewayRequestAttributes.TENANT_ID)),
        resolveCorrelationId(exchange),
        resolveClientIp(exchange.getRequest()),
        signal.getType().name(),
        errorMessage);
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
    InetSocketAddress remoteAddress = request.getRemoteAddress();
    if (remoteAddress != null) {
      InetAddress address = remoteAddress.getAddress();
      if (address != null) {
        return address.getHostAddress();
      }
      String hostString = remoteAddress.getHostString();
      if (StringUtils.hasText(hostString)) {
        return hostString;
      }
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
