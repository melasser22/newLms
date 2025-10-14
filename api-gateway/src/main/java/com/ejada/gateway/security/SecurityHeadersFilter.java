package com.ejada.gateway.security;

import com.ejada.gateway.config.GatewaySecurityProperties;
import java.util.Objects;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Applies hardened HTTP response headers based on OWASP recommendations.
 */
@Order(Ordered.LOWEST_PRECEDENCE - 50)
public class SecurityHeadersFilter implements GlobalFilter, Ordered {

  private final GatewaySecurityProperties properties;

  public SecurityHeadersFilter(GatewaySecurityProperties properties) {
    this.properties = Objects.requireNonNull(properties, "properties");
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    GatewaySecurityProperties.SecurityHeaders cfg = properties.getSecurityHeaders();
    if (!cfg.isEnabled()) {
      return chain.filter(exchange);
    }
    ServerHttpResponse response = exchange.getResponse();
    response.beforeCommit(() -> {
      HttpHeaders headers = response.getHeaders();
      headers.set("Content-Security-Policy", cfg.getContentSecurityPolicy());
      headers.set("X-Content-Type-Options", "nosniff");
      headers.set("X-Frame-Options", cfg.getFrameOptions());
      headers.set("Referrer-Policy", cfg.getReferrerPolicy());
      headers.set("Permissions-Policy", cfg.getPermissionsPolicy());
      headers.set("X-XSS-Protection", "0");
      if (cfg.isHstsEnabled() && isHttps(exchange)) {
        StringBuilder hsts = new StringBuilder("max-age=").append(cfg.getHstsMaxAgeSeconds());
        if (cfg.isHstsIncludeSubdomains()) {
          hsts.append("; includeSubDomains");
        }
        if (cfg.isHstsPreload()) {
          hsts.append("; preload");
        }
        headers.set("Strict-Transport-Security", hsts.toString());
      }
      if (cfg.isRemoveServerHeader()) {
        headers.remove(HttpHeaders.SERVER);
      }
      return Mono.empty();
    });
    return chain.filter(exchange);
  }

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE - 50;
  }

  private boolean isHttps(ServerWebExchange exchange) {
    HttpStatus status = exchange.getResponse().getStatusCode();
    if (status != null && status.is3xxRedirection()) {
      return true;
    }
    String scheme = exchange.getRequest().getURI().getScheme();
    return "https".equalsIgnoreCase(scheme);
  }
}
