package com.ejada.starter_security;

import static org.assertj.core.api.Assertions.assertThat;

import com.ejada.common.context.ContextManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CrossTenantAccessFilterTest {

    private final SharedSecurityProps.TenantVerification verification = new SharedSecurityProps.TenantVerification();
    private final CrossTenantAccessFilter filter = new CrossTenantAccessFilter(verification, new com.fasterxml.jackson.databind.ObjectMapper());

    @AfterEach
    void clearContext() {
        ContextManager.Tenant.clear();
    }

    @Test
    void allowsMatchingTenantHeader() throws ServletException, IOException {
        ContextManager.Tenant.set("tenant-1");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "tenant-1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean();
        FilterChain chain = (req, res) -> invoked.set(true);

        filter.doFilter(request, response, chain);

        assertThat(invoked).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void blocksMismatchedTenantInPath() throws ServletException, IOException {
        ContextManager.Tenant.set("tenant-1");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/tenants/tenant-2/resources");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("Tenant mismatch");
    }

    @Test
    void blocksMismatchedTenantInJsonBody() throws ServletException, IOException {
        ContextManager.Tenant.set("tenant-1");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/data");
        request.setContentType("application/json");
        request.setContent("{\"tenantId\":\"tenant-2\"}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("tenant-1").contains("tenant-2");
    }
}
