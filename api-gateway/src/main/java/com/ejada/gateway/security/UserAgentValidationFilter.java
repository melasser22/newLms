package com.ejada.gateway.security;

import com.ejada.gateway.config.GatewaySecurityProperties;
import com.ejada.starter_core.web.FilterSkipUtils;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Rejects requests that originate from known bot user agents before reaching downstream
 * security layers.
 */
public class UserAgentValidationFilter implements WebFilter, Ordered {

  private final GatewaySecurityProperties.UserAgentValidation properties;
  private final List<Pattern> patterns;

  public UserAgentValidationFilter(GatewaySecurityProperties.UserAgentValidation properties) {
    this.properties = properties;
    this.patterns = buildPatterns(properties);
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 15;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    if (properties == null || !properties.isEnabled()) {
      return chain.filter(exchange);
    }
    String path = exchange.getRequest().getPath().pathWithinApplication().value();
    if (FilterSkipUtils.shouldSkip(path, properties.getSkipPatterns())) {
      return chain.filter(exchange);
    }
    String userAgent = exchange.getRequest().getHeaders().getFirst(HttpHeaders.USER_AGENT);
    if (!StringUtils.hasText(userAgent) || CollectionUtils.isEmpty(patterns)) {
      return chain.filter(exchange);
    }
    for (Pattern pattern : patterns) {
      if (pattern.matcher(userAgent).find()) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return exchange.getResponse().setComplete();
      }
    }
    return chain.filter(exchange);
  }

  private List<Pattern> buildPatterns(GatewaySecurityProperties.UserAgentValidation properties) {
    if (properties == null || properties.getBlockedPatterns() == null) {
      return List.of();
    }
    return properties.getBlockedPatterns().stream()
        .filter(StringUtils::hasText)
        .map(pattern -> Pattern.compile(pattern.trim()))
        .toList();
  }
}
