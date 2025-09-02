package com.ejada.starter_security;

import com.ejada.common.constants.HeaderNames;
import com.ejada.common.context.ContextManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtTenantFilterTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
        ContextManager.Tenant.clear();
    }

    @Test
    void setsTenantFromJwtAndEchoesHeader() throws ServletException, IOException {
        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(60), Map.of("alg","none"), Map.of("tenant","acme"));
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        JwtTenantFilter filter = new JwtTenantFilter("tenant");
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        FilterChain chain = (request, response) -> assertEquals("acme", ContextManager.Tenant.get());

        filter.doFilter(req, res, chain);

        assertEquals("acme", res.getHeader(HeaderNames.X_TENANT_ID));
        assertNull(ContextManager.Tenant.get());
    }
}
