package com.shared.starter_core.logging;

import com.common.constants.HeaderNames;
import com.shared.starter_core.context.TraceContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.util.AntPathMatcher;
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
    private final AntPathMatcher matcher = new AntPathMatcher();

    public CorrelationIdFilter(
            String headerName,
            String mdcKey,
            boolean generateIfMissing,
            boolean echoResponseHeader,
            String[] skipPatterns
    ) {
        this.headerName = Objects.requireNonNullElse(headerName, HeaderNames.CORRELATION_ID);
        this.mdcKey = Objects.requireNonNullElse(mdcKey, "correlationId");
        this.generateIfMissing = generateIfMissing;
        this.echoResponseHeader = echoResponseHeader;
        this.skipPatterns = (skipPatterns != null && skipPatterns.length > 0)
                ? skipPatterns
                : new String[]{"/actuator/**", "/swagger-ui/**", "/v3/api-docs/**", "/static/**", "/webjars/**", "/error", "/favicon.ico"};
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        for (String p : skipPatterns) {
            if (matcher.match(p, uri)) return true;
        }
        return false;
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
            TraceContextHolder.setTraceId(correlationId);
            // Set early to ensure response always has the header (even on exceptions)
            if (echoResponseHeader) {
                res.setHeader(headerName, correlationId);
            }
        }

        try {
            chain.doFilter(req, res);
        } finally {
            MDC.remove(mdcKey);
            TraceContextHolder.clear();
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
