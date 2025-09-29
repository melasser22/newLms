package com.ejada.crypto.starter.audit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ejada.common.constants.HeaderNames;
import com.ejada.common.context.ContextManager;
import com.ejada.common.context.CorrelationContextUtil;
import com.ejada.common.context.TenantMdcUtil;

import java.io.IOException;

/**
 * Ensures MDC has a correlation id for all logs.
 * Also propagates optional tenantId/userId if provided via headers or upstream MDC.
 *
 * MDC keys used:
 *  - correlationId
 *  - tenantId (optional)
 *  - userId   (optional)
 *
 * Default header names (override in your gateway if needed):
 *  - X-Correlation-Id
 *  - HeaderNames.X_TENANT_ID
 *  - X-User-Id
 */
public class CryptoMdcFilter extends OncePerRequestFilter {


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        // Read incoming headers
        String incomingCorrelation = headerOrNull(request, HeaderNames.CORRELATION_ID);
        String incomingTenant = headerOrNull(request, HeaderNames.X_TENANT_ID);
        String incomingUser = headerOrNull(request, HeaderNames.USER_ID);

        String correlationId = CorrelationContextUtil.init(incomingCorrelation);

        ContextManager.Tenant.Scope tenantScope = null;
        boolean tenantAddedToMdc = false;
        if (StringUtils.hasText(incomingTenant)) {
            tenantScope = ContextManager.Tenant.openScope(incomingTenant);
            if (!StringUtils.hasText(MDC.get(HeaderNames.X_TENANT_ID))) {
                TenantMdcUtil.setTenantId(incomingTenant);
                tenantAddedToMdc = true;
            }
        }

        boolean putUser = false;
        if (!StringUtils.hasText(MDC.get(HeaderNames.USER_ID)) && StringUtils.hasText(incomingUser)) {
            MDC.put(HeaderNames.USER_ID, incomingUser);
            putUser = true;
        }

        try {
            // Echo correlation id in response for client-side tracing
            response.setHeader(HeaderNames.CORRELATION_ID, correlationId);
            chain.doFilter(request, response);
        } finally {
            if (putUser) {
                MDC.remove(HeaderNames.USER_ID);
            }
            if (tenantAddedToMdc) {
                TenantMdcUtil.clear();
            }
            if (tenantScope != null) {
                tenantScope.close();
            }
            CorrelationContextUtil.clear();
        }
    }

    private static String headerOrNull(HttpServletRequest req, String name) {
        String v = req.getHeader(name);
        return StringUtils.hasText(v) ? v.trim() : null;
    }

}
