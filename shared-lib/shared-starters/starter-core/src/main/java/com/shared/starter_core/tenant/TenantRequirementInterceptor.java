package com.shared.starter_core.tenant;

import com.common.context.ContextManager;
import com.shared.starter_core.config.CoreAutoConfiguration.CoreProps;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;

public class TenantRequirementInterceptor implements HandlerInterceptor {

    private final String defaultPolicy;

    // >>> This is the constructor CoreAutoConfiguration calls <<<
    public TenantRequirementInterceptor(CoreProps.Tenant cfg) {
        this.defaultPolicy = cfg.getDefaultPolicy();
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod hm)) {
            return true;
        }

        boolean require = isAnnotated(hm, RequireTenant.class);
        boolean optional = isAnnotated(hm, OptionalTenant.class);

        if (!require && !optional) {
            require = "REQUIRED".equalsIgnoreCase(defaultPolicy);
        }
        if (optional) {
            require = false;
        }

        if (require && !ContextManager.Tenant.isPresent()) {
            writeTenantMissing(response);
            return false;
        }
        return true;
    }

    private static boolean isAnnotated(HandlerMethod hm, Class<? extends Annotation> ann) {
        return hm.hasMethodAnnotation(ann) || hm.getBeanType().isAnnotationPresent(ann);
    }

    private static void writeTenantMissing(HttpServletResponse res) throws IOException {
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        res.setContentType("application/json");
        String payload = """
            {"code":"TENANT_REQUIRED","message":"Tenant is required for this endpoint","status":400}
            """;
        res.getOutputStream().write(payload.getBytes(StandardCharsets.UTF_8));
    }
}
