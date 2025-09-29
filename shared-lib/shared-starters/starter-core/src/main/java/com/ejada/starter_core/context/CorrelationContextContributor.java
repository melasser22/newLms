package com.ejada.starter_core.context;

import com.ejada.common.constants.HeaderNames;
import com.ejada.common.context.ContextManager;
import com.ejada.common.context.CorrelationContextUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.Objects;
import java.util.UUID;

/**
 * Handles correlation id propagation: reads from incoming headers, generates if
 * missing (configurable) and ensures both MDC and response headers are updated.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationContextContributor implements RequestContextContributor {

    private final String headerName;
    private final String mdcKey;
    private final boolean generateIfMissing;
    private final boolean echoResponseHeader;

    public CorrelationContextContributor(String headerName,
                                         String mdcKey,
                                         boolean generateIfMissing,
                                         boolean echoResponseHeader) {
        this.headerName = Objects.requireNonNullElse(headerName, HeaderNames.CORRELATION_ID);
        this.mdcKey = Objects.requireNonNullElse(mdcKey, HeaderNames.CORRELATION_ID);
        this.generateIfMissing = generateIfMissing;
        this.echoResponseHeader = echoResponseHeader;
    }

    @Override
    public ContextScope contribute(HttpServletRequest request, HttpServletResponse response) {
        String correlationId = trimToNull(request.getHeader(headerName));
        if (correlationId == null && generateIfMissing) {
            correlationId = UUID.randomUUID().toString();
        }
        if (correlationId == null) {
            return ContextScope.noop();
        }

        final String cid = correlationId;
        MDC.put(mdcKey, cid);
        MDC.put(HeaderNames.CORRELATION_ID, cid);
        CorrelationContextUtil.put(HeaderNames.CORRELATION_ID, cid);
        ContextManager.setCorrelationId(cid);
        if (echoResponseHeader) {
            response.setHeader(headerName, cid);
        }

        return () -> {
            MDC.remove(mdcKey);
            MDC.remove(HeaderNames.CORRELATION_ID);
            ContextManager.clearCorrelationId();
        };
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
