package com.ejada.starter_core.tenant;

import com.ejada.common.context.ContextManager;
import com.ejada.starter_core.config.CoreAutoConfiguration.CoreProps;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.lang.annotation.Annotation;

public class TenantRequirementInterceptor implements WebFilter, Ordered {

    private final String defaultPolicy;
    private final int order;

    // >>> This is the constructor CoreAutoConfiguration calls <<<
    public TenantRequirementInterceptor(CoreProps.Tenant cfg) {
        this.defaultPolicy = cfg.getDefaultPolicy();
        this.order = cfg.getOrder() + 1;
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        Object handlerObj = exchange.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
        HandlerMethod hm = (handlerObj instanceof HandlerMethod) ? (HandlerMethod) handlerObj : null;

        boolean require = false;
        boolean optional = false;
        if (hm != null) {
            require = isAnnotated(hm, RequireTenant.class);
            optional = isAnnotated(hm, OptionalTenant.class);
        }

        if (!require && !optional) {
            require = "REQUIRED".equalsIgnoreCase(defaultPolicy);
        }
        if (optional) {
            require = false;
        }

        if (require && !ContextManager.Tenant.isPresent()) {
            var res = exchange.getResponse();
            res.setStatusCode(HttpStatus.BAD_REQUEST);
            res.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            byte[] payload = ("{" +
                    "\"code\":\"TENANT_REQUIRED\"," +
                    "\"message\":\"Tenant is required for this endpoint\"," +
                    "\"status\":400" +
                    "}").getBytes(StandardCharsets.UTF_8);
            return res.writeWith(Mono.just(res.bufferFactory().wrap(payload)));
        }

        return chain.filter(exchange);
    }

    private static boolean isAnnotated(HandlerMethod hm, Class<? extends Annotation> ann) {
        return hm.hasMethodAnnotation(ann) || hm.getBeanType().isAnnotationPresent(ann);
    }
}
