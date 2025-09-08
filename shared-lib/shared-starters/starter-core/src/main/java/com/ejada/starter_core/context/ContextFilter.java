package com.ejada.starter_core.context;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import com.ejada.common.constants.HeaderNames;
import com.ejada.common.context.ContextManager;
import com.ejada.common.context.CorrelationContextUtil;

import java.util.Set;
import java.util.regex.Pattern;

public class ContextFilter implements WebFilter, Ordered {


    // Paths we don't want to enforce tenant/correlation for
    private static final Set<String> SKIP_PREFIXES = Set.of(
        "/actuator", "/health", "/error", "/v3/api-docs", "/swagger", "/swagger-ui", "/docs"
    );

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        final String path = exchange.getRequest().getPath().value();
        for (String p : SKIP_PREFIXES) {
            if (path.startsWith(p)) {
                return chain.filter(exchange);
            }
        }

        String tenantId = trimToNull(firstNonNull(
                exchange.getRequest().getHeaders().getFirst(HeaderNames.X_TENANT_ID),
                exchange.getRequest().getQueryParams().getFirst(HeaderNames.X_TENANT_ID)
        ));
        if (tenantId != null && !TENANT_PATTERN.matcher(tenantId).matches()) {
            exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
            return exchange.getResponse().setComplete();
        }
        String incomingCorrelation = trimToNull(
                exchange.getRequest().getHeaders().getFirst(HeaderNames.CORRELATION_ID)
        );
        String userId = trimToNull(firstNonNull(
                exchange.getRequest().getHeaders().getFirst(HeaderNames.USER_ID),
                null
        ));

        CorrelationContextUtil.init(incomingCorrelation, tenantId);
        String correlationId = CorrelationContextUtil.getCorrelationId();

        try {
            if (tenantId != null) {
                ContextManager.Tenant.set(tenantId);
            }
            putMdc(HeaderNames.X_TENANT_ID, tenantId);
            putMdc(HeaderNames.USER_ID, userId);
            putMdc(HeaderNames.CORRELATION_ID, correlationId);

            exchange.getResponse().getHeaders().set(HeaderNames.CORRELATION_ID, correlationId);

            return chain.filter(exchange)
                    .doFinally(s -> {
                        ContextManager.Tenant.clear();
                        CorrelationContextUtil.clear();
                        MDC.remove(HeaderNames.X_TENANT_ID);
                        MDC.remove(HeaderNames.USER_ID);
                        MDC.remove(HeaderNames.CORRELATION_ID);
                    });
        } catch (RuntimeException ex) {
            ContextManager.Tenant.clear();
            CorrelationContextUtil.clear();
            MDC.remove(HeaderNames.X_TENANT_ID);
            MDC.remove(HeaderNames.USER_ID);
            MDC.remove(HeaderNames.CORRELATION_ID);
            throw ex;
        }
    }

    // ---------- Helpers

    private static String firstNonNull(String a, String b) {
        return a != null ? a : b;
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
