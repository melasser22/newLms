package com.ejada.gateway.security;

import com.ejada.common.constants.HeaderNames;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Writes the CSRF token value to the {@code X-CSRF-Token} response header so that
 * JavaScript clients can capture it on the first request and reuse it for
 * subsequent state-changing calls.
 */
public class CsrfTokenResponseWebFilter implements WebFilter {

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    Mono<CsrfToken> token = exchange.getAttribute(CsrfToken.class.getName());
    if (token == null) {
      return chain.filter(exchange);
    }
    return token.doOnNext(csrfToken -> exchange.getResponse().getHeaders()
            .set(HeaderNames.CSRF_TOKEN, csrfToken.getToken()))
        .then(chain.filter(exchange));
  }
}

