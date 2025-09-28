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
        Jwt jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(60),
                Map.of("alg", "none"),
                Map.of("tenant", "acme", "uid", 42L)
        );
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        JwtTenantFilter filter = new JwtTenantFilter("tenant");
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        FilterChain chain = (request, response) -> {
            assertEquals("acme", ContextManager.Tenant.get());
            assertEquals("acme", request.getAttribute(HeaderNames.X_TENANT_ID));
            assertEquals("42", ContextManager.getUserId());
        };

        filter.doFilter(req, res, chain);

        assertEquals("acme", res.getHeader(HeaderNames.X_TENANT_ID));
        assertNull(ContextManager.Tenant.get());
        assertNull(ContextManager.getUserId());
    }

    @Test
    void supportsNumericTenantClaim() throws ServletException, IOException {
        Jwt jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(60),
                Map.of("alg", "none"),
                Map.of("tenant", 1234L)
        );
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        JwtTenantFilter filter = new JwtTenantFilter("tenant");
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, (request, response) -> assertEquals("1234", ContextManager.Tenant.get()));

        assertEquals("1234", res.getHeader(HeaderNames.X_TENANT_ID));
    }

    @Test
    void marksSuperAdminWithoutTenant() throws ServletException, IOException {
        Jwt jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(60),
                Map.of("alg", "none"),
                Map.of("isSuperadmin", true, "uid", "admin-1")
        );
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        JwtTenantFilter filter = new JwtTenantFilter("tenant");
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, (request, response) -> assertNull(ContextManager.Tenant.get()));

        assertEquals("true", res.getHeader("X-Is-Superadmin"));
        assertNull(res.getHeader(HeaderNames.X_TENANT_ID));
    }
}
