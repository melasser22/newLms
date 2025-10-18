package com.ejada.shared_starter_ratelimit;

import com.ejada.common.context.ContextManager;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Reactive {@link WebFilter} enforcing rate limits using {@link RateLimitService}.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitWebFilter implements WebFilter {

  private final RateLimitService rateLimitService;

  public RateLimitWebFilter(RateLimitService rateLimitService) {
    this.rateLimitService = rateLimitService;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    return exchange.getPrincipal()
        .filter(Authentication.class::isInstance)
        .cast(Authentication.class)
        .map(Optional::of)
        .defaultIfEmpty(Optional.empty())
        .flatMap(authOptional -> evaluate(exchange, chain, authOptional.orElse(null)));
  }

  private Mono<Void> evaluate(ServerWebExchange exchange, WebFilterChain chain, Authentication authentication) {
    ServerHttpRequest request = exchange.getRequest();
    String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";
    String path = request.getPath().pathWithinApplication().value();
    RateLimitEvaluationRequest evaluationRequest = new RateLimitEvaluationRequest(
        ContextManager.Tenant.get(),
        ContextManager.getUserId(),
        resolveIp(request),
        method + " " + path,
        authentication);

    RateLimitDecision decision = rateLimitService.evaluate(evaluationRequest);
    applyHeaders(exchange.getResponse(), decision);

    if (!decision.allowed() && !decision.isBypass()) {
      return writeRejection(exchange, decision);
    }

    return chain.filter(exchange);
  }

  private void applyHeaders(ServerHttpResponse response, RateLimitDecision decision) {
    HttpHeaders headers = response.getHeaders();
    RateLimitTier tier = decision.tier();
    headers.set("X-RateLimit-Tier", tier.name());
    headers.set("X-RateLimit-Limit", String.valueOf(tier.requestsPerMinute()));
    headers.set("X-RateLimit-Burst", String.valueOf(tier.burstCapacity()));
    if (decision.isBypass()) {
      headers.set("X-RateLimit-Bypass", "true");
      headers.set("X-RateLimit-Bypass-Reason", decision.bypassDecision().reasonCode());
    } else {
      long remaining = Math.max(0L, Math.round(decision.remainingTokens()));
      headers.set("X-RateLimit-Remaining", String.valueOf(remaining));
      RateLimitStrategy strategy = decision.strategy();
      if (strategy != null) {
        headers.set("X-RateLimit-Strategy", strategy.name());
      }
    }
  }

  private Mono<Void> writeRejection(ServerWebExchange exchange, RateLimitDecision decision) {
    ServerHttpResponse response = exchange.getResponse();
    response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
    HttpHeaders headers = response.getHeaders();
    Duration retryAfter = Optional.ofNullable(decision.retryAfter()).orElse(Duration.ZERO);
    if (!retryAfter.isZero()) {
      long seconds = Math.max(1L, retryAfter.toSeconds());
      headers.set("Retry-After", String.valueOf(seconds));
      headers.set("X-RateLimit-Reset", String.valueOf(seconds));
    }
    headers.set("X-RateLimit-Reason", decision.reason().code());
    headers.setContentType(MediaType.APPLICATION_JSON);

    String body = "{\"error\":\"rate_limited\",\"reason\":\"" + decision.reason().code()
        + "\",\"message\":\"Rate limit exceeded\"}";
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    DataBuffer buffer = response.bufferFactory().wrap(bytes);
    return response.writeWith(Mono.just(buffer));
  }

  private String resolveIp(ServerHttpRequest request) {
    String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      int comma = forwarded.indexOf(',');
      return comma >= 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
    }
    if (request.getRemoteAddress() != null && request.getRemoteAddress().getAddress() != null) {
      return request.getRemoteAddress().getAddress().getHostAddress();
    }
    return "unknown";
  }
}

