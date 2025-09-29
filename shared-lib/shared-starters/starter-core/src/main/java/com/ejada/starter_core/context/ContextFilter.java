package com.ejada.starter_core.context;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ejada.common.constants.HeaderNames;
import com.ejada.common.context.ContextManager;
import com.ejada.common.context.CorrelationContextUtil;
import com.ejada.starter_core.tenant.TenantResolution;
import com.ejada.starter_core.tenant.TenantResolver;
import com.ejada.starter_core.web.FilterSkipUtils;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class ContextFilter extends OncePerRequestFilter {

    private final String[] skipPatterns;
    private TenantResolver tenantResolver;

    public ContextFilter() {
        this(TenantResolver.noop(), FilterSkipUtils.defaultPatterns());
    }

    public ContextFilter(TenantResolver tenantResolver) {
        this(tenantResolver, FilterSkipUtils.defaultPatterns());
    }

    public ContextFilter(TenantResolver tenantResolver, String[] skipPatterns) {
        this.skipPatterns = FilterSkipUtils.copyOrDefault(skipPatterns);
        this.tenantResolver = tenantResolver != null ? tenantResolver : TenantResolver.noop();
    }

    @Autowired(required = false)
    public void setTenantResolver(TenantResolver tenantResolver) {
        if (tenantResolver != null) {
            this.tenantResolver = tenantResolver;
        }
    }


    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return FilterSkipUtils.shouldSkip(request.getRequestURI(), skipPatterns);
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        TenantResolution tenantResolution = tenantResolver.resolve(request);
        if (tenantResolution.isInvalid()) {

            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid " + HeaderNames.X_TENANT_ID);
            return;
        }
        String tenantId = tenantResolution.tenantId();
        String incomingCorrelation = trimToNull(
                request.getHeader(HeaderNames.CORRELATION_ID)
        );
        String userId        = trimToNull(firstNonNull(
                request.getHeader(HeaderNames.USER_ID),
                (String) request.getAttribute(HeaderNames.USER_ID)  // some stacks set it as an attribute
        ));

        // Initialize correlation (generates a new one if missing) and tenant context
        CorrelationContextUtil.init(incomingCorrelation, tenantId);
        String correlationId = CorrelationContextUtil.getCorrelationId();

        // If you want hard enforcement for protected APIs, uncomment:
        // if (tenantId == null) {
        //     response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing " + HDR_TENANT_ID);
        //     return;
        // }

        try (ContextManager.Tenant.Scope ignored = ContextManager.Tenant.openScope(tenantId)) {
            // ---- Enrich logging context (appears on every log line)
            putMdc(HeaderNames.X_TENANT_ID, tenantId);
            putMdc(HeaderNames.USER_ID, userId);
            putMdc(HeaderNames.CORRELATION_ID, correlationId);

            // ---- Echo correlation for clients & downstream services
            response.setHeader(HeaderNames.CORRELATION_ID, correlationId);

            filterChain.doFilter(request, response);
        } finally {
            // ---- Always cleanup
            CorrelationContextUtil.clear();
            MDC.remove(HeaderNames.X_TENANT_ID);
            MDC.remove(HeaderNames.USER_ID);
            MDC.remove(HeaderNames.CORRELATION_ID);
        }
    }

    // ---------- Helpers

    private static String firstNonNull(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static void putMdc(String key, String value) {
        if (value != null) MDC.put(key, value);
    }
}
