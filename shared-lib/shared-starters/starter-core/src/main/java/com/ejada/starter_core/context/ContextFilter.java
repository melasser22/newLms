package com.ejada.starter_core.context;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ejada.common.constants.HeaderNames;
import com.ejada.common.context.ContextManager;
import com.ejada.common.context.CorrelationContextUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ContextFilter extends OncePerRequestFilter {


    // Paths we don't want to enforce tenant/correlation for
    private static final Set<String> SKIP_PREFIXES = Set.of(
        "/actuator", "/health", "/error", "/v3/api-docs", "/swagger", "/swagger-ui", "/docs"
    );

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        final String path = request.getRequestURI();
        for (String p : SKIP_PREFIXES) {
            if (path.startsWith(p)) return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String tenantId      = trimToNull(firstNonNull(
                request.getHeader(HeaderNames.X_TENANT_ID),
                request.getHeader(HeaderNames.X_TENANT_ID_LEGACY),
                request.getParameter(HeaderNames.X_TENANT_ID),           // optional fallback
                request.getParameter(HeaderNames.X_TENANT_ID_LEGACY)
        ));
        if (tenantId != null && !TENANT_PATTERN.matcher(tenantId).matches()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid " + HeaderNames.X_TENANT_ID);
            return;
        }
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

        try {
            // ---- Propagate to thread-local holders used elsewhere in the stack
            if (tenantId != null) {
                ContextManager.Tenant.set(tenantId);
            }
            // ---- Enrich logging context (appears on every log line)
            putMdc(HeaderNames.X_TENANT_ID, tenantId);
            putMdc(HeaderNames.USER_ID, userId);
            putMdc(HeaderNames.CORRELATION_ID, correlationId);

            // ---- Echo correlation for clients & downstream services
            response.setHeader(HeaderNames.CORRELATION_ID, correlationId);

            filterChain.doFilter(request, response);
        } finally {
            // ---- Always cleanup
            ContextManager.Tenant.clear();
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

    private static final Pattern TENANT_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,36}");
}
