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

        if (!isBlank(correlationId)) {
            MDC.put(mdcKey, correlationId);
            CorrelationContextUtil.put(CorrelationContextUtil.CORRELATION_ID, correlationId);
            // Set early to ensure response always has the header (even on exceptions)
            if (echoResponseHeader) {
                res.setHeader(headerName, correlationId);
            }
        }

        try {
            chain.doFilter(req, res);
        } finally {
            MDC.remove(mdcKey);
            CorrelationContextUtil.clear();
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
