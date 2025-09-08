package com.ejada.starter_security;

import com.ejada.common.constants.HeaderNames;
import com.ejada.common.context.ContextManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtTenantFilterTest {

    @AfterEach
    void clearContext() {
        ContextManager.Tenant.clear();
    }

    @Test
    void setsTenantFromJwtAndEchoesHeader() {
        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(60), Map.of("alg","none"), Map.of("tenant","acme"));
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);

        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build());
        exchange = exchange.mutate().principal(Mono.just(auth)).build();
        WebFilterChain chain = ex -> Mono.fromRunnable(() -> assertEquals("acme", ContextManager.Tenant.get()));

        new JwtTenantFilter("tenant").filter(exchange, chain).block();

        assertEquals("acme", exchange.getResponse().getHeaders().getFirst(HeaderNames.X_TENANT_ID));
        assertNull(ContextManager.Tenant.get());
    }
}
