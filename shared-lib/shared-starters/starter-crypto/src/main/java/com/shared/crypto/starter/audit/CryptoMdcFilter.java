package com.shared.crypto.starter.audit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.common.constants.HeaderNames;

import java.io.IOException;
import java.util.UUID;

/**
 * Ensures MDC has a correlation id (traceId) for all logs.
 * Also propagates optional tenantId/userId if provided via headers or upstream MDC.
 *
 * MDC keys used:
 *  - traceId
 *  - tenantId (optional)
 *  - userId   (optional)
 *
 * Default header names (override in your gateway if needed):
 *  - X-Correlation-Id
 *  - HeaderNames.TENANT_ID
 *  - X-User-Id
 */
public class CryptoMdcFilter extends OncePerRequestFilter {


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        // Read incoming headers
        String incomingTrace = headerOrNull(request, HeaderNames.CORRELATION_ID);
        String incomingTenant = headerOrNull(request, HeaderNames.TENANT_ID);
        String incomingUser = headerOrNull(request, HeaderNames.USER_ID);

        // Prefer existing MDC (if upstream filter already set it)
        boolean putTrace = false, putTenant = false, putUser = false;

        if (!StringUtils.hasText(MDC.get(HeaderNames.TRACE_ID))) {
            String traceId = StringUtils.hasText(incomingTrace) ? incomingTrace : genTraceId();
            MDC.put(HeaderNames.TRACE_ID, traceId);
            putTrace = true;
        }

        if (!StringUtils.hasText(MDC.get(HeaderNames.TENANT_ID)) && StringUtils.hasText(incomingTenant)) {
            MDC.put(HeaderNames.TENANT_ID, incomingTenant);
            putTenant = true;
        }

        if (!StringUtils.hasText(MDC.get(HeaderNames.TENANT_ID)) && StringUtils.hasText(incomingUser)) {
            MDC.put(HeaderNames.TENANT_ID, incomingUser);
            putUser = true;
        }

        try {
            // Echo correlation id in response for client-side tracing
            response.setHeader(HeaderNames.CORRELATION_ID, MDC.get(HeaderNames.TENANT_ID));
            chain.doFilter(request, response);
        } finally {
            // Clean up only keys we added (don’t clobber upstream MDC)
            if (putTrace)  MDC.remove(HeaderNames.TENANT_ID);
            if (putTenant) MDC.remove(HeaderNames.TENANT_ID);
            if (putUser)   MDC.remove(HeaderNames.USER_ID);
        }
    }

    private static String headerOrNull(HttpServletRequest req, String name) {
        String v = req.getHeader(name);
        return StringUtils.hasText(v) ? v.trim() : null;
    }

    private static String genTraceId() {
        // UUID v4 is fine for correlation; you can swap with ULID if preferred
        return UUID.randomUUID().toString();
    }
}
