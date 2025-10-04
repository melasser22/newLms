package com.ejada.gateway.filter;

import com.ejada.gateway.config.GatewayWebsocketProperties;
import com.ejada.gateway.config.GatewayWebsocketProperties.WebsocketRoute;
import com.ejada.gateway.context.GatewayRequestAttributes;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Enforces per-tenant WebSocket connection limits and ensures tenant context is preserved for the
 * lifetime of the WebSocket session.
 */
@Component
public class WebsocketConnectionLimiterFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(WebsocketConnectionLimiterFilter.class);

  private final GatewayWebsocketProperties properties;
  private final ConcurrentMap<String, WebsocketRoute> validatedRoutes = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, AtomicInteger> activeConnections = new ConcurrentHashMap<>();

  public WebsocketConnectionLimiterFilter(GatewayWebsocketProperties properties) {
    this.properties = Objects.requireNonNull(properties, "properties");
  }

  public GatewayFilter apply(String routeId) {
    return new RouteLimiter(routeId);
  }

  private WebsocketRoute resolveRoute(String routeId) {
    if (!properties.isEnabled()) {
      return null;
    }
    WebsocketRoute cached = validatedRoutes.get(routeId);
    if (cached != null) {
      return cached;
    }
    Optional<WebsocketRoute> optional = properties.findRoute(routeId);
    if (optional.isEmpty()) {
      return null;
    }
    WebsocketRoute route = optional.get();
    route.validate(routeId);
    validatedRoutes.put(routeId, route);
    return route;
  }

  private class RouteLimiter implements GatewayFilter {

    private final String routeId;

    RouteLimiter(String routeId) {
      this.routeId = routeId;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
      WebsocketRoute route = resolveRoute(routeId);
      if (route == null || !ServerWebExchangeUtils.isWebSocketUpgrade(exchange.getRequest())) {
        return chain.filter(exchange);
      }

      String tenant = resolveTenant(exchange);
      AtomicInteger counter = activeConnections.computeIfAbsent(tenant, key -> new AtomicInteger());
      int current = counter.incrementAndGet();
      if (current > properties.getMaxConnectionsPerTenant()) {
        counter.decrementAndGet();
        LOGGER.warn("Tenant {} exceeded WebSocket connection limit ({} > {})", tenant, current, properties.getMaxConnectionsPerTenant());
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        return exchange.getResponse().setComplete();
      }

      exchange.getAttributes().put(GatewayRequestAttributes.TENANT_ID, tenant);

      return chain.filter(exchange)
          .doFinally(signalType -> {
            int remaining = counter.decrementAndGet();
            if (remaining <= 0) {
              activeConnections.remove(tenant, counter);
            }
          });
    }
  }

  private String resolveTenant(ServerWebExchange exchange) {
    Object attribute = exchange.getAttribute(GatewayRequestAttributes.TENANT_ID);
    if (attribute != null) {
      String tenant = Objects.toString(attribute, null);
      if (StringUtils.hasText(tenant)) {
        return tenant;
      }
    }
    String header = exchange.getRequest().getHeaders().getFirst("X-Tenant-Id");
    if (StringUtils.hasText(header)) {
      return header.trim();
    }
    return "anonymous";
  }
}

