package com.ejada.starter_security;

import com.ejada.common.constants.HeaderNames;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.web.server.csrf.DefaultCsrfToken;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;

class CsrfHeaderFilterTest {

    @Test
    void copiesTokenToHeader() {
        CsrfToken token = new DefaultCsrfToken("X-CSRF-Token", "_csrf", "abc123");
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build());
        exchange.getAttributes().put(CsrfToken.class.getName(), Mono.just(token));
        WebFilterChain chain = ex -> Mono.empty();
        new CsrfHeaderFilter().filter(exchange, chain).block();
        assertEquals("abc123", exchange.getResponse().getHeaders().getFirst(HeaderNames.CSRF_TOKEN));
    }

    @Test
    void skipsWhenNoToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build());
        WebFilterChain chain = ex -> Mono.empty();
        new CsrfHeaderFilter().filter(exchange, chain).block();
        assertNull(exchange.getResponse().getHeaders().getFirst(HeaderNames.CSRF_TOKEN));
    }
}
