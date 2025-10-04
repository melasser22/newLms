package com.ejada.gateway.filter;

import com.ejada.common.constants.HeaderNames;
import com.ejada.common.context.ContextManager;
import com.ejada.gateway.config.GatewayLoggingProperties;
import com.ejada.gateway.context.GatewayRequestAttributes;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Emits structured JSON access logs for each gateway request.
 */
public class GatewayAccessLogFilter implements WebFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(GatewayAccessLogFilter.class);

  private final ObjectMapper objectMapper;
  private final List<String> skipPatterns;
  private final AntPathMatcher pathMatcher = new AntPathMatcher();

  public GatewayAccessLogFilter(ObjectMapper objectMapper, GatewayLoggingProperties.AccessLog properties) {
    this.objectMapper = objectMapper;
    this.skipPatterns = List.copyOf(properties.getSkipPatterns());
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String path = exchange.getRequest().getURI().getPath();
    if (shouldSkip(path)) {
      return chain.filter(exchange);
    }
    long start = System.nanoTime();
    return chain.filter(exchange)
        .doFinally(signalType -> emitLog(exchange, start));
  }

  private boolean shouldSkip(String path) {
    if (!StringUtils.hasText(path)) {
      return false;
    }
    for (String pattern : skipPatterns) {
      if (pathMatcher.match(pattern, path)) {
        return true;
      }
    }
    return false;
  }

  private void emitLog(ServerWebExchange exchange, long startNanos) {
    if (!LOGGER.isInfoEnabled()) {
      return;
    }
    Map<String, Object> payload = buildPayload(exchange, startNanos);
    try {
      LOGGER.info(objectMapper.writeValueAsString(payload));
    } catch (JsonProcessingException e) {
      LOGGER.info("{}", payload);
    }
  }

  private Map<String, Object> buildPayload(ServerWebExchange exchange, long startNanos) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("timestamp", Instant.now().toString());
    payload.put("correlationId", resolveCorrelationId(exchange));
    payload.put("tenantId", resolveTenantId(exchange));
    ServerHttpRequest request = exchange.getRequest();
    payload.put("method", request.getMethodValue());
    payload.put("path", request.getURI().getRawPath());
    int status = exchange.getResponse().getRawStatusCode() != null
        ? exchange.getResponse().getRawStatusCode()
        : 200;
    payload.put("statusCode", status);
    payload.put("duration", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
    payload.put("userAgent", request.getHeaders().getFirst(HeaderNames.USER_AGENT));
    payload.put("clientIp", resolveClientIp(request));
    payload.put("routeId", resolveRouteId(exchange));
    return payload;
  }

  private String resolveCorrelationId(ServerWebExchange exchange) {
    String attribute = exchange.getAttribute(GatewayRequestAttributes.CORRELATION_ID);
    if (StringUtils.hasText(attribute)) {
      return attribute;
    }
    String header = exchange.getRequest().getHeaders().getFirst(HeaderNames.CORRELATION_ID);
    if (StringUtils.hasText(header)) {
      return header;
    }
    return ContextManager.getCorrelationId();
  }

  private String resolveTenantId(ServerWebExchange exchange) {
    String attribute = exchange.getAttribute(GatewayRequestAttributes.TENANT_ID);
    if (StringUtils.hasText(attribute)) {
      return attribute;
    }
    String header = exchange.getRequest().getHeaders().getFirst(HeaderNames.X_TENANT_ID);
    if (StringUtils.hasText(header)) {
      return header;
    }
    return ContextManager.Tenant.get();
  }

  private String resolveClientIp(ServerHttpRequest request) {
    String forwarded = request.getHeaders().getFirst(HeaderNames.CLIENT_IP);
    if (StringUtils.hasText(forwarded)) {
      return forwarded;
    }
    InetSocketAddress remoteAddress = request.getRemoteAddress();
    return remoteAddress != null ? remoteAddress.getHostString() : "unknown";
  }

  private String resolveRouteId(ServerWebExchange exchange) {
    Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
    if (route != null && StringUtils.hasText(route.getId())) {
      return route.getId();
    }
    return "unknown";
  }
}
