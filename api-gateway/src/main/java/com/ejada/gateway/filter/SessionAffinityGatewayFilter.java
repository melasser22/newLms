package com.ejada.gateway.filter;

import com.ejada.gateway.config.GatewayRoutesProperties;
import java.util.Objects;
import java.util.UUID;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Lightweight session affinity helper that ensures a stable affinity identifier is propagated for
 * routes that require sticky sessions. The gateway emits/echoes a cookie and mirrors it to a
 * configurable header so downstream services or load balancers can honour the affinity.
 */
public class SessionAffinityGatewayFilter implements GatewayFilter {

  private final GatewayRoutesProperties.ServiceRoute.SessionAffinity affinity;

  public SessionAffinityGatewayFilter(GatewayRoutesProperties.ServiceRoute.SessionAffinity affinity) {
    this.affinity = Objects.requireNonNull(affinity, "affinity");
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    if (!affinity.isEnabled()) {
      return chain.filter(exchange);
    }

    String affinityId = resolveAffinityId(exchange);
    boolean generated = false;
    if (!StringUtils.hasText(affinityId)) {
      affinityId = UUID.randomUUID().toString();
      generated = true;
    }

    ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate();
    String headerName = affinity.getHeaderName();
    if (StringUtils.hasText(headerName)) {
      final String headerValue = affinityId;
      requestBuilder.headers(httpHeaders -> {
        if (!httpHeaders.containsKey(headerName)) {
          httpHeaders.add(headerName, headerValue);
        }
      });
    }

    ServerWebExchange mutated = exchange.mutate()
        .request(requestBuilder.build())
        .build();

    if (generated || !hasAffinityCookie(exchange)) {
      ResponseCookie cookie = ResponseCookie.from(affinity.getCookieName(), affinityId)
          .path("/")
          .httpOnly(false)
          .secure(false)
          .sameSite("Lax")
          .build();
      mutated.getResponse().addCookie(cookie);
    }

    return chain.filter(mutated);
  }

  private String resolveAffinityId(ServerWebExchange exchange) {
    var request = exchange.getRequest();
    var cookie = request.getCookies().getFirst(affinity.getCookieName());
    if (cookie != null && StringUtils.hasText(cookie.getValue())) {
      return cookie.getValue();
    }
    String header = request.getHeaders().getFirst(affinity.getHeaderName());
    return StringUtils.hasText(header) ? header : null;
  }

  private boolean hasAffinityCookie(ServerWebExchange exchange) {
    var cookie = exchange.getRequest().getCookies().getFirst(affinity.getCookieName());
    return cookie != null && StringUtils.hasText(cookie.getValue());
  }
}
