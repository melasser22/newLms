package com.ejada.starter_security;

import com.ejada.common.constants.HeaderNames;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Exposes the CSRF token value via {@code X-CSRF-Token} header so that
 * JavaScript clients can read it on the first request and echo it back in
 * subsequent state changing requests.
 */
class CsrfHeaderFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        Mono<CsrfToken> token = exchange.getAttributeOrDefault(CsrfToken.class.getName(), Mono.empty());
        return token.doOnSuccess(t -> {
                    if (t != null) {
                        exchange.getResponse().getHeaders().set(HeaderNames.CSRF_TOKEN, t.getToken());
                    }
                })
                .then(chain.filter(exchange));
    }
}
