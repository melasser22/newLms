package com.ejada.shared_starter_ratelimit;

import com.ejada.common.context.ContextManager;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Servlet filter enforcing rate limits using the {@link RateLimitService}.
 */
public class RateLimitFilter implements Filter {

  private final RateLimitService rateLimitService;

  public RateLimitFilter(RateLimitService rateLimitService) {
    this.rateLimitService = rateLimitService;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    RateLimitEvaluationRequest evaluationRequest = new RateLimitEvaluationRequest(
        ContextManager.Tenant.get(),
        ContextManager.getUserId(),
        resolveIp(httpRequest),
        httpRequest.getMethod() + " " + httpRequest.getRequestURI(),
        authentication);

    RateLimitDecision decision = rateLimitService.evaluate(evaluationRequest);
    applyHeaders(httpResponse, decision);

    if (!decision.allowed() && !decision.isBypass()) {
      writeRejection(httpResponse, decision);
      return;
    }

    chain.doFilter(request, response);
  }

  private void applyHeaders(HttpServletResponse response, RateLimitDecision decision) {
    RateLimitTier tier = decision.tier();
    response.setHeader("X-RateLimit-Tier", tier.name());
    response.setHeader("X-RateLimit-Limit", String.valueOf(tier.requestsPerMinute()));
    response.setHeader("X-RateLimit-Burst", String.valueOf(tier.burstCapacity()));
    if (decision.isBypass()) {
      response.setHeader("X-RateLimit-Bypass", "true");
      response.setHeader("X-RateLimit-Bypass-Reason", decision.bypassDecision().reasonCode());
    } else {
      long remaining = Math.max(0L, Math.round(decision.remainingTokens()));
      response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
      RateLimitStrategy strategy = decision.strategy();
      if (strategy != null) {
        response.setHeader("X-RateLimit-Strategy", strategy.name());
      }
    }
  }

  private void writeRejection(HttpServletResponse response, RateLimitDecision decision) throws IOException {
    response.setStatus(429);
    Duration retryAfter = Optional.ofNullable(decision.retryAfter()).orElse(Duration.ZERO);
    if (!retryAfter.isZero()) {
      long seconds = Math.max(1L, retryAfter.toSeconds());
      response.setHeader("Retry-After", String.valueOf(seconds));
      response.setHeader("X-RateLimit-Reset", String.valueOf(seconds));
    }
    response.setHeader("X-RateLimit-Reason", decision.reason().code());
    response.setContentType("application/json");
    String body = "{\"error\":\"rate_limited\",\"reason\":\"" + decision.reason().code()
        + "\",\"message\":\"Rate limit exceeded\"}";
    response.getWriter().write(body);
  }

  private String resolveIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      int comma = forwarded.indexOf(',');
      return comma >= 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
    }
    String remoteAddr = request.getRemoteAddr();
    return remoteAddr == null ? "unknown" : remoteAddr;
  }
}
