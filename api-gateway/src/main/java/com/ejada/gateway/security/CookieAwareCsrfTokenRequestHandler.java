package com.ejada.gateway.security;

import com.ejada.common.constants.HeaderNames;
import java.util.List;
import org.springframework.http.HttpCookie;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.security.web.server.csrf.ServerCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Extends the default request handler so that a CSRF token delivered via cookie
 * is still accepted even when the {@code X-CSRF-Token} header is missing. This
 * matches how the gateway exposes the token to browser clients and prevents
 * inadvertent 403 responses on first login attempts.
 */
public class CookieAwareCsrfTokenRequestHandler extends ServerCsrfTokenRequestAttributeHandler {

  private static final List<String> CSRF_COOKIE_NAMES = List.of("XSRF-TOKEN", HeaderNames.CSRF_TOKEN);

  @Override
  public Mono<String> resolveCsrfTokenValue(ServerWebExchange exchange, CsrfToken csrfToken) {
    return super.resolveCsrfTokenValue(exchange, csrfToken)
        .switchIfEmpty(Mono.defer(() -> Mono.justOrEmpty(resolveFromCookies(exchange))));
  }

  private String resolveFromCookies(ServerWebExchange exchange) {
    return CSRF_COOKIE_NAMES.stream()
        .map(name -> exchange.getRequest().getCookies().getFirst(name))
        .filter(cookie -> cookie != null && StringUtils.hasText(cookie.getValue()))
        .map(HttpCookie::getValue)
        .findFirst()
        .orElse(null);
  }
}
