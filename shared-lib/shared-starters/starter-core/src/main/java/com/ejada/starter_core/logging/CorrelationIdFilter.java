package com.ejada.starter_core.logging;

import com.ejada.common.constants.HeaderNames;
import com.ejada.common.context.CorrelationContextUtil;
import com.ejada.starter_core.web.FilterSkipUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

/**
 * @deprecated Use {@link com.ejada.starter_core.context.ContextFilter} with a
 * {@link com.ejada.starter_core.context.CorrelationContextContributor}
 * instead. The dedicated filter will be removed in a future release.
 */
@Deprecated(since = "1.6.0", forRemoval = true)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private final String headerName;
    private final String mdcKey;
    private final boolean generateIfMissing;
    private final boolean echoResponseHeader;
    private final String[] skipPatterns;

    public CorrelationIdFilter(
            String headerName,
            String mdcKey,
            boolean generateIfMissing,
            boolean echoResponseHeader,
            String[] skipPatterns
    ) {
        this.headerName = Objects.requireNonNullElse(headerName, HeaderNames.CORRELATION_ID);
        this.mdcKey = Objects.requireNonNullElse(mdcKey, HeaderNames.CORRELATION_ID);
        this.generateIfMissing = generateIfMissing;
        this.echoResponseHeader = echoResponseHeader;
        this.skipPatterns = FilterSkipUtils.copyOrDefault(skipPatterns);

    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return FilterSkipUtils.shouldSkip(request.getRequestURI(), skipPatterns);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String correlationId = req.getHeader(headerName);
        if (isBlank(correlationId) && generateIfMissing) {
            correlationId = UUID.randomUUID().toString();
        }

        boolean correlationApplied = false;
        if (!isBlank(correlationId)) {
            correlationApplied = true;
            MDC.put(mdcKey, correlationId);
            CorrelationContextUtil.setCorrelationId(correlationId);
            // Set early to ensure response always has the header (even on exceptions)
            if (echoResponseHeader) {
                res.setHeader(headerName, correlationId);
            }
        }

        try {
            chain.doFilter(req, res);
        } finally {
            MDC.remove(mdcKey);
            if (correlationApplied) {
                CorrelationContextUtil.clear();
            }
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
